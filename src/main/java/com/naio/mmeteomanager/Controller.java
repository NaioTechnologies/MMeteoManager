package com.naio.mmeteomanager;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.JDBCConnectionException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.naio.mmeteopersistence.entity.MMeteoBoxData;
import com.naio.mmeteopersistence.entity.Naio;
import com.naio.util.Mail;
import com.naio.util.PropertyManager;


public class Controller
{
	public static final String MMETEOMANAGER = "mmeteomanager";
	
	private static final Logger logger = Logger.getLogger(Controller.class);
	
	Session _Session;
	DataManager _DataManager;
	MailController _MailController;
	PropertyManager _PropertyManager;
	SmsManager _SmsManager;
	ReportManager _ReportManager;
	
	Boolean jdbcExceptionAlreadySent;
	
	public Controller()
	{
		jdbcExceptionAlreadySent = false;
	}

	@SuppressWarnings("unused")
	public void doStuff(String confFolderPath, String reportPath, String tempPath )
	{
		try
		{
			logger.info(Controller.class.getPackage().getImplementationVersion());
			
			this._PropertyManager = new PropertyManager();
			
			this._PropertyManager.load(confFolderPath);
			
			this._SmsManager = new SmsManager(this._PropertyManager);
			
			this._DataManager = new DataManager(Integer.parseInt(this._PropertyManager.getValue( PropertyManager.ACTIVITY_MAX_DURATION_MINUTE ) ), Integer.parseInt( this._PropertyManager.getValue( PropertyManager.LAST_CHECK_MAX_DURATION_MINUTE ) ) );
			
			this._MailController = new MailController(this._PropertyManager);
			
			this._MailController.mailLog("INFO", "Starting MMeteoManager", "Starting MMeteoManager");
				
			this._ReportManager = new ReportManager(reportPath, tempPath);
			
			while(true)
			{
				try
				{
					this._PropertyManager.load(confFolderPath);
					
					this._Session =  HibernateUtil.getSessionFactory( confFolderPath ).openSession();
					
					if( this._Session != null && this._Session.isConnected() )
					{
						this._ReportManager.checkReportGeneration(this._Session, this._MailController);
						
						List<Mail> mailList = this._MailController.readAndForwardReceivedMail();
						
						this._DataManager.manageLastincomingData(this._Session, this._MailController);
						
						List<MMeteoBoxData> mmeteoBoxDataActivityAlertList = this._DataManager.checkMMeteoActiviy(this._Session);
						
						this.SendActivityAlert(this._Session, mmeteoBoxDataActivityAlertList);
												
						this._Session.close();
						
						jdbcExceptionAlreadySent = false;
					}
					
					Thread.sleep(10000);
				}
				catch(Exception Ex)
				{
					logger.error(Ex.getMessage());
					
					if( Ex instanceof JDBCConnectionException && !jdbcExceptionAlreadySent )
					{
						this._MailController.mailLog( "ERROR", "JDBCConnectionException : check MMeteo DB", Ex.getMessage() );
						
						jdbcExceptionAlreadySent = true;
					}
					else
					{
						try
						{
							if( this._Session != null )
							{
								if( this._Session.getTransaction() != null )
								{
									this._Session.getTransaction().rollback();
								}
								
								if( this._Session.isConnected() )
								{
									this._Session.close();
								}
							}
						}
						catch(Exception dbEx )
						{
							logger.error(dbEx);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
			
			this._MailController.mailLog( "ERROR", "MMeteoManager.Controller.doStuff()", e.getMessage() );
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	public void SendActivityAlert( Session session, List<MMeteoBoxData> mmeteoBoxDataActivityAlertList )
	{
		Transaction transaction = null;

		try
		{
			if( mmeteoBoxDataActivityAlertList.size() > 0 )
			{
				transaction  = session.beginTransaction();
				
				String message = "Warning Activity Alert for the following M.Meteo Box :\n\n";
				
				String smsMessage = "Activity Alert :\n";
				
				Boolean sendSms = false;
				
				for( MMeteoBoxData mmeteoBoxData : mmeteoBoxDataActivityAlertList )
				{
					DateTime now = new DateTime();
					
					smsMessage = smsMessage + "GMT Date : " + now.toDateTime(DateTimeZone.UTC).toString("yyyy/MM/dd HH:mm:ss") + "\n";
					smsMessage = smsMessage + "Ref : " + mmeteoBoxData.getMmeteoBox().getReferenceNaio() + "\n";
					smsMessage = smsMessage + "IMEI : " + mmeteoBoxData.getMmeteoBox().getImei() + "\n";
					smsMessage = smsMessage + "Tel 1 : " + mmeteoBoxData.getMmeteoBox().getClientPhone1() + "\n";
					smsMessage = smsMessage + "Tel 2 : " + mmeteoBoxData.getMmeteoBox().getClientPhone2() + "\n";
					smsMessage = smsMessage + "Tel 3 : " + mmeteoBoxData.getMmeteoBox().getClientPhone3() + "\n";
					
					message = message + "########################################" + "\n\n";
					message = message + "GMT Date : " + now.toDateTime(DateTimeZone.UTC).toString("yyyy/MM/dd HH:mm:ss") + "\n";
					message = message + "Ref : " + mmeteoBoxData.getMmeteoBox().getReferenceNaio() + "\n";
					message = message + "IMEI : " + mmeteoBoxData.getMmeteoBox().getImei() + "\n"; 
					message = message + "Date Version : " + mmeteoBoxData.getMmeteoBox().getMmeteoDateVersion() + "\n";
					message = message + "Soft Version : " + mmeteoBoxData.getMmeteoBox().getSoftVersion() + "\n";
					message = message + "PhoneNumber : " + mmeteoBoxData.getMmeteoBox().getPhoneNumber() + "\n";
					message = message + "Client Phone 1 : " + mmeteoBoxData.getMmeteoBox().getClientPhone1() + "\n";
					message = message + "Client Phone 2 : " + mmeteoBoxData.getMmeteoBox().getClientPhone2() + "\n";
					message = message + "Client Phone 3 : " + mmeteoBoxData.getMmeteoBox().getClientPhone3() + "\n";
					//message = message + "Last activity Date : " + mmeteoBox.getLastIncomingMessageDate().toString() + "\n";
					
					if( mmeteoBoxData.getMmeteoBox().getCustomer() != null )
					{
						smsMessage = smsMessage + "Name : " + mmeteoBoxData.getMmeteoBox().getCustomer().getName() + "\n";
						smsMessage = smsMessage + "City : " + mmeteoBoxData.getMmeteoBox().getCustomer().getCity() + "\n";
						smsMessage = smsMessage + "Tel. : " + mmeteoBoxData.getMmeteoBox().getCustomer().getPhoneNumber() + "\n";
						smsMessage = smsMessage + "\n";
						
						message = message + "Customer Social : " +mmeteoBoxData.getMmeteoBox().getCustomer().getSocial() + "\n";
						message = message + "Customer Name : " + mmeteoBoxData.getMmeteoBox().getCustomer().getName() + "\n";
						message = message + "Customer Address : " + mmeteoBoxData.getMmeteoBox().getCustomer().getAddress() + "\n";
						message = message + "Customer Postal Code : " + mmeteoBoxData.getMmeteoBox().getCustomer().getPostalCode() + "\n";
						message = message + "Customer City : " + mmeteoBoxData.getMmeteoBox().getCustomer().getCity() + "\n";
						message = message + "Customer Phone : " +mmeteoBoxData.getMmeteoBox().getCustomer().getPhoneNumber() + "\n";
						message = message + "Customer Status : " +mmeteoBoxData.getMmeteoBox().getCustomer().getCustomerStatus().toString() + "\n";
					}
					
					if( mmeteoBoxData.getAlertSmsSent() == false && !mmeteoBoxData.getMmeteoBox().getClientPhone1().startsWith("X") )
					{
						String telPhone = mmeteoBoxData.getMmeteoBox().getClientPhone1() ;
						
						if( !telPhone.startsWith( "+" ) )
						{
							if( telPhone.startsWith( "0" ) )
							{
								telPhone.replaceFirst( "0", "+33" );
							}
						}
						
						String clientMessage = "Votre boitier m.meteo " + mmeteoBoxData.getMmeteoBox().getPhoneNumber() + " ne reponds pas.\nVeuillez verifier l'alimentation electrique.\n";

						logger.error( clientMessage );
						
						Boolean smsServerAuthenticated = this._SmsManager.authenticate();
						
						if( smsServerAuthenticated )
						{
							this._SmsManager.sendSms( telPhone , clientMessage );
						}
						
						mmeteoBoxData.setAlertSmsSent(true);
						
						session.save(mmeteoBoxData);
						
						sendSms = true;
					}
				}
				
				message = message + "\n########################################" + "\n\n";
								
				Criteria naioCriteria = session.createCriteria(Naio.class);
				
				List<Naio> naioList = (List<Naio>)(naioCriteria.list()); 
				
				for( Naio naio : naioList )
				{
					if( naio.getSendEmailAlert() == true )
					{
						this._MailController.sendMail( naio.getEmail(), "MMeteo Activity Alert", message );
					}
					
					if( sendSms == true && naio.getSendSmsAlert() == true )
					{
						try
						{
							Boolean smsServerAuthenticated = this._SmsManager.authenticate();
							
							if( smsServerAuthenticated )
							{
								this._SmsManager.sendSms( naio.getAlertPhoneNumber() , smsMessage );
							}
						}
						catch(Exception smsEx)
						{
							logger.error("SMS sending error : " + smsEx.getMessage() );
						}
					}					
				}
				
				transaction.commit();
			}
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
	}
}
