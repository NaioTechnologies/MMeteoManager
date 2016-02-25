package com.naio.mmeteomanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.naio.mmeteopersistence.entity.MMeteoBox;
import com.naio.mmeteopersistence.entity.MMeteoBoxData;
import com.naio.mmeteopersistence.entity.MeteoData;
import com.naio.mmeteopersistence.entity.MeteoDataArchive;
import com.naio.mmeteopersistence.entity.Naio;
import com.naio.mmeteopersistence.dao.CustomerDao;
import com.naio.mmeteopersistence.dao.MMeteoBoxDataDao;

public class DataManager
{
	private static final Logger logger = Logger.getLogger(DataManager.class);
	
	private static final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
	
	private int activityMaxDurationMin; 
	private int lastCheckMaxDurationMinute;
	
	
	public DataManager(int activityMaxDurationMin, int lastCheckMaxDurationMinute )
	{
		this.activityMaxDurationMin = activityMaxDurationMin;
		
		this.lastCheckMaxDurationMinute = lastCheckMaxDurationMinute;
	}
	
	public List<MMeteoBoxData> checkMMeteoActiviy(Session session)
	{
		List<MMeteoBoxData> mmeteoBoxDataActivityAlertList = new ArrayList<MMeteoBoxData>();
		
		Transaction transaction = null;
		
		MMeteoBoxDataDao myMMeteoBoxDataDao = new MMeteoBoxDataDao();
		
		try
		{
			transaction  = session.beginTransaction();
			 
			List<MMeteoBoxData> mmeteoBoxDataList =  myMMeteoBoxDataDao.findNoActiveBox( session );
						
			for( MMeteoBoxData mmeteoBoxData : mmeteoBoxDataList )
			{
				DateTime lastCheckMin = new DateTime().toDateTime(DateTimeZone.UTC);
				long tempTime = new DateTime().toDateTime(DateTimeZone.UTC).getMillis();
			
				lastCheckMin =  new DateTime( tempTime - (  mmeteoBoxData.getMmeteoBox().getAlertTimeoutMinute() * ONE_MINUTE_IN_MILLIS ), DateTimeZone.UTC );
				
				if( mmeteoBoxData.getMmeteoBox().getActivityCheck() == true && ( mmeteoBoxData.getLastActivityCheckTime() == null || lastCheckMin.getMillis() >  mmeteoBoxData.getLastActivityCheckTime() ) )
				{
					logger.info("MMeteoBox lost activity : " + mmeteoBoxData.getMmeteoBox().getImei() + " ; Last incoming message date was : " +  mmeteoBoxData.getLastIncomingMessageTime() );
					
					logger.info( " ACTIVITY_MAX_DURATION_MINUTE = " + String.valueOf( this.activityMaxDurationMin ) + " ; LAST_CHECK_MAX_DURATION_MINUTE = " + String.valueOf( this.lastCheckMaxDurationMinute ) ); 
					
					if( mmeteoBoxData.getMmeteoBox().getCustomer() != null )
					{
						logger.info("MMeteoBox lost activity check customer : " + mmeteoBoxData.getMmeteoBox().getCustomer().getName() );
					}
					
					mmeteoBoxData.setLastActivityCheckTime( new DateTime(DateTimeZone.UTC).toDate().getTime() );
					
					//mmeteoBoxData.setAlertSmsSent( true );
					
					session.save(mmeteoBoxData);
					
					mmeteoBoxDataActivityAlertList.add(mmeteoBoxData);
				}
			}
			
			transaction.commit();
		}
		catch(Exception e)
		{
			logger.error( e.getMessage() );
			
			if( transaction != null )
			{
				transaction.rollback();
			}
		}
		finally
		{
			
		}
		
		return mmeteoBoxDataActivityAlertList;
	}
	
		
	@SuppressWarnings("unchecked")
	public void manageLastincomingData(Session session, MailController mailController)
	{
		int nbLinesArchived = 0;
		
		Transaction transaction  = null;

		MMeteoBoxDataDao myMMeteoBoxDataDao = new MMeteoBoxDataDao();
		CustomerDao customerDao = new CustomerDao();
		
		try
		{
			transaction  = session.beginTransaction();
			
			Criteria criteria = session.createCriteria(MeteoData.class);
			
			criteria.addOrder(Order.asc(MeteoData.PROP_TS));
			
			criteria.setMaxResults(100);
			
			List<MeteoData> meteoDataList = criteria.list();
			
			// #######################################
			// METEO_BOX
			
			for( MeteoData meteoData : meteoDataList )
			{
				Boolean saveMMeteoBox = false;
				Boolean newClientRegistered = false;
				
				// compute data to be stores in mmeteo_box
				if( meteoData.getImei() != null  )
				{
					MMeteoBoxData myMMeteoBoxData = myMMeteoBoxDataDao.findByImei( session, meteoData.getImei() );
					
					MMeteoBox myMMeteoBox = null; 
					
					if( myMMeteoBoxData == null )
					{
						logger.info("New MMeteoBox imei detected : " + meteoData.getImei() );
						logger.info("Create automatically a new MMeteoBox.");
						
						myMMeteoBox = new MMeteoBox();
						
						myMMeteoBox.setActivityCheck(false);
						myMMeteoBox.setTemperatureMin(0);
						myMMeteoBox.setTemperatureMax(100);
						myMMeteoBox.setHumidityMin(0);
						myMMeteoBox.setHumidityMax(100);
						myMMeteoBox.setImei(meteoData.getImei());
						myMMeteoBox.setOwner(Controller.MMETEOMANAGER);
						myMMeteoBox.setCustomer( customerDao.getDefaultCustomer( session ) );
						myMMeteoBox.setReferenceNaio("AUTO_CREATION");
						myMMeteoBox.setFirstActivityTime(new DateTime(DateTimeZone.UTC).toDate().getTime());
						myMMeteoBox.setActivityTimeoutMinute( this.activityMaxDurationMin );
						myMMeteoBox.setAlertTimeoutMinute( this.lastCheckMaxDurationMinute );

						myMMeteoBox.setClientPhone1("X");
						myMMeteoBox.setClientPhone2("X");
						myMMeteoBox.setClientPhone3("X");
						
						myMMeteoBox.setDailyReportHour(10);
						myMMeteoBox.setWeeklyReportDay(1);
						myMMeteoBox.setMonthlyReportDay(1);
						
						myMMeteoBox.setSendDailyReport(false);
						myMMeteoBox.setSendWeeklyReport(false);
						myMMeteoBox.setSendMonthlyReport(false);
						
						myMMeteoBox.setGpsTime((new DateTime(DateTimeZone.UTC)).toDate().getTime());
						myMMeteoBox.setGpsLat(43.563290);
						myMMeteoBox.setGpsLon(1.491713);
						myMMeteoBox.setGpsAlt(180.0);
						
						myMMeteoBox.setRegistered(false);
												
						saveMMeteoBox = true;

						myMMeteoBoxData = new MMeteoBoxData();
						
						myMMeteoBoxData.setLastActivityCheckTime(null);
						myMMeteoBoxData.setSmsReceived(0);
						myMMeteoBoxData.setSmsSent(0);
						myMMeteoBoxData.setBytesReceived(0);
						myMMeteoBoxData.setBytesSent(0);
						myMMeteoBoxData.setMmeteoBox(myMMeteoBox);
					}
					else
					{
						myMMeteoBox = myMMeteoBoxData.getMmeteoBox();
					}
				

					// reset alert flag
					myMMeteoBoxData.setAlertSmsSent(false);
					
					if( meteoData.getMmeteoSoftVersion() != myMMeteoBox.getSoftVersion() )
					{
						myMMeteoBox.setSoftVersion(meteoData.getMmeteoSoftVersion());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getMmeteoDateVersion() != myMMeteoBox.getMmeteoDateVersion() )
					{
						myMMeteoBox.setMmeteoDateVersion(meteoData.getMmeteoDateVersion());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getPhoneNumber() != myMMeteoBox.getPhoneNumber() )
					{
						myMMeteoBox.setPhoneNumber(meteoData.getPhoneNumber());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getClientPhone1() != myMMeteoBox.getClientPhone1() && meteoData.getClientPhone1() != null && myMMeteoBox.getClientPhone1() != null )
					{
						// Alert naios if new phone number is added on the mmeteo box
						if( !meteoData.getClientPhone1().equals( "X" ) && myMMeteoBox.getClientPhone1().equals( "X" ) )
						{
							newClientRegistered = true;
						}
						
						myMMeteoBox.setClientPhone1(meteoData.getClientPhone1());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getClientPhone2() != myMMeteoBox.getClientPhone2() )
					{
						myMMeteoBox.setClientPhone2(meteoData.getClientPhone2());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getClientPhone3() != myMMeteoBox.getClientPhone3() )
					{
						myMMeteoBox.setClientPhone3(meteoData.getClientPhone3());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getMmeteoDateVersion() != myMMeteoBox.getMmeteoDateVersion() )
					{
						myMMeteoBox.setMmeteoDateVersion(meteoData.getMmeteoDateVersion());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getTemperatureMin() != null &&  meteoData.getTemperatureMin() != myMMeteoBoxData.getMmeteoBox().getTemperatureMin() )
					{
						myMMeteoBox.setTemperatureMin(meteoData.getTemperatureMin());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getTemperatureMax() != null && meteoData.getTemperatureMax() != myMMeteoBoxData.getMmeteoBox().getTemperatureMax() )
					{
						myMMeteoBox.setTemperatureMax(meteoData.getTemperatureMax());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getHumidityMin() != null && meteoData.getHumidityMin() != myMMeteoBoxData.getMmeteoBox().getHumidityMin() )
					{
						myMMeteoBox.setHumidityMin(meteoData.getHumidityMin());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getHumidityMax() != null && meteoData.getHumidityMax() != myMMeteoBoxData.getMmeteoBox().getHumidityMax() )
					{
						myMMeteoBox.setHumidityMax(meteoData.getHumidityMax());
						saveMMeteoBox = true;
					}
					
					if( meteoData.getSmsReceived() != null )
					{
						myMMeteoBoxData.setSmsReceived(meteoData.getSmsReceived());
					}
					
					if( meteoData.getSmsSent() != null )
					{
						myMMeteoBoxData.setSmsSent(meteoData.getSmsSent());
					}
					
					if( meteoData.getBytesReceived() != null )
					{
						myMMeteoBoxData.setBytesReceived(meteoData.getBytesReceived());
					}
					
					if( meteoData.getBytesSent() != null )
					{
						myMMeteoBoxData.setBytesSent(meteoData.getBytesSent());
					}

					myMMeteoBoxData.setLastHumidity(meteoData.getDataHumidity());
					
					myMMeteoBoxData.setLastIncomingMessageTime( meteoData.getTs() );
					
					myMMeteoBoxData.setLastTemperature(meteoData.getDataTemperature());
					
					myMMeteoBoxData.setSignalStrength( meteoData.getSignalStrength() );
					
					if( saveMMeteoBox )
					{
						session.save(myMMeteoBox);
					}

					session.save(myMMeteoBoxData);
					
					if( newClientRegistered == true && myMMeteoBox.getRegistered() == false )
					{
						this.sendNewMMeteoCientConnectedMail(session, myMMeteoBoxData, mailController);
						
						//myMMeteoBox.setRegistered(true);
						
						//session.save(myMMeteoBox);
					}
				}
									
				// #######################################
				// METEO_DATA_ARCHIVE
				
				// copy data to archive for further explotation
				MeteoDataArchive myMeteoDataArchive = new MeteoDataArchive();
				
				if(  meteoData.getId() != null )
				{
					myMeteoDataArchive.setId( meteoData.getId().toString() );
				}
				
				myMeteoDataArchive.setBytesReceived(meteoData.getBytesReceived());
				myMeteoDataArchive.setBytesSent(meteoData.getBytesSent());
				
				if( meteoData.getClientPhone1() == null || meteoData.getClientPhone1().startsWith("X") )
				{
					myMeteoDataArchive.setClientPhone1(null);	
				}
				else
				{
					myMeteoDataArchive.setClientPhone1(meteoData.getClientPhone1());
				}
				
				if( meteoData.getClientPhone2() == null || meteoData.getClientPhone2().startsWith("X") )
				{
					myMeteoDataArchive.setClientPhone2(null);	
				}
				else
				{
					myMeteoDataArchive.setClientPhone2(meteoData.getClientPhone2());
				}

				if( meteoData.getClientPhone3() == null || meteoData.getClientPhone3().startsWith("X") )
				{
					myMeteoDataArchive.setClientPhone3(null);	
				}
				else
				{
					myMeteoDataArchive.setClientPhone3(meteoData.getClientPhone3());
				}
				
				myMeteoDataArchive.setDataTime(meteoData.getDataTime());
				myMeteoDataArchive.setDataHumidity(meteoData.getDataHumidity());
				myMeteoDataArchive.setDataTemperature(meteoData.getDataTemperature());
				
				myMeteoDataArchive.setHumidityMax(meteoData.getHumidityMax());
				myMeteoDataArchive.setHumidityMin(meteoData.getHumidityMin());
				
				myMeteoDataArchive.setImei(meteoData.getImei());
				
				myMeteoDataArchive.setMmeteoDateVersion(meteoData.getMmeteoDateVersion());
				myMeteoDataArchive.setMmeteoSoftVersion(meteoData.getMmeteoSoftVersion());
				
				myMeteoDataArchive.setOwner(meteoData.getOwner());
				
				myMeteoDataArchive.setPhoneNumber(meteoData.getPhoneNumber());
				
				myMeteoDataArchive.setSmsReceived(meteoData.getSmsReceived());
				myMeteoDataArchive.setSmsSent(meteoData.getSmsSent());
				
				myMeteoDataArchive.setTemperatureMax(meteoData.getTemperatureMax());
				myMeteoDataArchive.setTemperatureMin(meteoData.getTemperatureMin());
				myMeteoDataArchive.setTs(meteoData.getTs());
				
				myMeteoDataArchive.setSignalStrength(meteoData.getSignalStrength());
				
				nbLinesArchived++;
				
				session.save(myMeteoDataArchive);
				
				session.delete(meteoData);
			}
			
			transaction.commit();
			
			if( nbLinesArchived > 0 )
			{
				logger.info("Lines archives from meteo_data : " + nbLinesArchived);
			}
		}
		catch(Exception e)
		{
			logger.error( e.getMessage() );
			
			transaction.rollback();
		}
		finally
		{
			
		}
	}
	
	private void sendNewMMeteoCientConnectedMail( Session session, MMeteoBoxData mmeteoBoxData, MailController mailController )
	{
		DateTime now = new DateTime();
			
		String message = "NEW MMETEO CLIENT ON : " + mmeteoBoxData.getMmeteoBox().getImei() + " CALL : " + mmeteoBoxData.getMmeteoBox().getClientPhone1() + "\n\n";

		logger.info(message);
		
		message = message + "Europa/Barcelona Date : " + now.toDateTime(DateTimeZone.forID("Europe/Paris")).toString("yyyy/MM/dd HH:mm:ss") + "\n";
		message = message + "Ref : " + mmeteoBoxData.getMmeteoBox().getReferenceNaio() + "\n";
		message = message + "IMEI : " + mmeteoBoxData.getMmeteoBox().getImei() + "\n"; 
		message = message + "Date Version : " + mmeteoBoxData.getMmeteoBox().getMmeteoDateVersion() + "\n";
		message = message + "Soft Version : " + mmeteoBoxData.getMmeteoBox().getSoftVersion() + "\n";
		message = message + "PhoneNumber : " + mmeteoBoxData.getMmeteoBox().getPhoneNumber() + "\n";
		message = message + "Client Phone 1 : " + mmeteoBoxData.getMmeteoBox().getClientPhone1() + "\n";
		message = message + "Client Phone 2 : " + mmeteoBoxData.getMmeteoBox().getClientPhone2() + "\n";
		message = message + "Client Phone 3 : " + mmeteoBoxData.getMmeteoBox().getClientPhone3() + "\n";
		
		if( mmeteoBoxData.getMmeteoBox().getCustomer() != null )
		{
			message = message + "Customer Social : " +mmeteoBoxData.getMmeteoBox().getCustomer().getSocial() + "\n";
			message = message + "Customer Name : " + mmeteoBoxData.getMmeteoBox().getCustomer().getName() + "\n";
			message = message + "Customer Address : " + mmeteoBoxData.getMmeteoBox().getCustomer().getAddress() + "\n";
			message = message + "Customer Postal Code : " + mmeteoBoxData.getMmeteoBox().getCustomer().getPostalCode() + "\n";
			message = message + "Customer City : " + mmeteoBoxData.getMmeteoBox().getCustomer().getCity() + "\n";
			message = message + "Customer Phone : " +mmeteoBoxData.getMmeteoBox().getCustomer().getPhoneNumber() + "\n";
			message = message + "Customer Status : " +mmeteoBoxData.getMmeteoBox().getCustomer().getCustomerStatus().toString() + "\n";
		}

		Criteria naioCriteria = session.createCriteria(Naio.class);
		
		@SuppressWarnings("unchecked")
		List<Naio> naioList = (List<Naio>)(naioCriteria.list()); 
		
		for( Naio naio : naioList )
		{
			if( naio.getSendEmailAlert() == true )
			{
				try
				{
					mailController.sendMail( naio.getEmail(), "NEW MMETEO CLIENT ON : " + mmeteoBoxData.getMmeteoBox().getImei() + " CALL : " + mmeteoBoxData.getMmeteoBox().getClientPhone1(), message );
				}
				catch( Exception e )
				{
					logger.error( e.getMessage() );
				}
			}
		}
	}
	
}
