package com.naio.mmeteomanager;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.naio.mmeteopersistence.entity.MMeteoBox;

public class ReportManager
{
	private static final Logger logger = Logger.getLogger(ReportManager.class);
	
	String _ReportPath;
	String _TempPath;
	
	Boolean _GenerationError;
	
	public ReportManager(String reportPath, String tempPath)
	{
		this._ReportPath = reportPath;
		
		this._TempPath = tempPath;
	}
	
	@SuppressWarnings("unchecked")
	public void checkReportGeneration( Session session, MailController mailController )
	{
		try
		{
			Criteria mmeteoBoxCriteria = session.createCriteria(MMeteoBox.class);
			
			for( MMeteoBox mmeteoBox : (ArrayList<MMeteoBox>)(mmeteoBoxCriteria.list()) )
			{
				this.checkNextReportGeneration( session, mmeteoBox, mailController );
				
				this.checkReportGeneration( session, mmeteoBox, mailController );
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}
	
	private void checkNextReportGeneration( Session session, MMeteoBox mmeteoBox, MailController mailController )
	{
		Transaction transaction  = session.beginTransaction();
		
		try
		{
			DateTime now = new DateTime( DateTimeZone.UTC );
			
			if( mmeteoBox.getNextDailyReportTime() == null )
			{
				this.sendReport(session, mmeteoBox, now.minusDays( 1 ), now, mailController, null );
				
				DateTime next = now.withHourOfDay( mmeteoBox.getDailyReportHour() );
				
				next = next.withMinuteOfHour(0);
				next = next.withSecondOfMinute(0);
				next = next.withMillisOfSecond(0);
											
				mmeteoBox.setNextDailyReportTime( next.toDate().getTime() );
				
				session.save(mmeteoBox);
			}
		
			if( mmeteoBox.getNextWeeklyReportTime() == null )
			{
				this.sendReport(session, mmeteoBox, now.minusWeeks( 1 ), now, mailController, new Float(1.0) );
				
				DateTime next = now.withHourOfDay( mmeteoBox.getDailyReportHour() );
				
				next = next.withDayOfWeek( mmeteoBox.getWeeklyReportDay() );
				
				next = next.withMinuteOfHour(0);
				next = next.withSecondOfMinute(0);
				next = next.withMillisOfSecond(0);
								
				mmeteoBox.setNextWeeklyReportTime( next.toDate().getTime() );
				
				session.save(mmeteoBox);
			}
			
			if( mmeteoBox.getNextMonthlyReportTime() == null )
			{
				this.sendReport(session, mmeteoBox, now.minusMonths( 1 ), now, mailController, new Float(4.0) );
				
				DateTime next = now.withHourOfDay( mmeteoBox.getDailyReportHour() );
				
				next = next.withDayOfMonth( mmeteoBox.getMonthlyReportDay() );
				
				next = next.withMinuteOfHour(0);
				next = next.withSecondOfMinute(0);
				next = next.withMillisOfSecond(0);
								
				mmeteoBox.setNextMonthlyReportTime( next.toDate().getTime() );
			}
		
			transaction.commit();
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
	
	
	private void checkReportGeneration(Session session, MMeteoBox mmeteoBox, MailController mailController )
	{
		DateTime now = new DateTime( DateTimeZone.UTC );
		
		Transaction transaction  = session.beginTransaction();
		
		try
		{
			if( mmeteoBox.getSendDailyReport() == true )
			{
				DateTime next = new DateTime( mmeteoBox.getNextDailyReportTime(), DateTimeZone.UTC );
				
				if( now.isAfter( next ) )
				{
					Boolean sent = this.sendReport(session, mmeteoBox, next.minusHours(24), next, mailController, null );
					
					if( sent )
					{
						next = next.plusHours(24);
						
						mmeteoBox.setNextDailyReportTime(  next.toDate().getTime() );
						
						session.save(mmeteoBox);
					}
				}
			}
			
			if( mmeteoBox.getSendWeeklyReport() == true )
			{
				DateTime next = new DateTime( mmeteoBox.getNextWeeklyReportTime(), DateTimeZone.UTC );
				
				if( now.isAfter( next ) )
				{
					Boolean sent = this.sendReport(session, mmeteoBox, next.minusDays(7), next, mailController, new Float(1.0) );
					
					if( sent )
					{
						next = next.plusDays( 7 );
						
						mmeteoBox.setNextWeeklyReportTime( next.toDate().getTime() );
						
						session.save(mmeteoBox);
					}
				}
			}
			
			if( mmeteoBox.getSendMonthlyReport() == true )
			{
				DateTime next = new DateTime( mmeteoBox.getNextMonthlyReportTime(), DateTimeZone.UTC );
				
				if( now.isAfter( next ) )
				{
					Boolean sent = this.sendReport(session, mmeteoBox, next.minusMonths( 1 ).withDayOfMonth( mmeteoBox.getMonthlyReportDay() ), next, mailController, new Float(4.0) );
					
					if( sent )
					{
						next = next.plusMonths( 1 );
						next = next.withDayOfMonth( mmeteoBox.getMonthlyReportDay() );
						
						mmeteoBox.setNextMonthlyReportTime( next.toDate().getTime() );
						
						session.save(mmeteoBox);
					}
				}
			}
			
			transaction.commit();
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
	
	private Boolean sendReport(Session session, MMeteoBox mmeteoBox, DateTime fromDate, DateTime toDate, MailController mailController, Float hourInterval )
	{
		Boolean sent = false;

		try
		{
			if( mmeteoBox.getCustomer().getContactEmail() == "" || mmeteoBox.getCustomer().getContactEmail() == null )
			{
				logger.warn("No contact email defined for sending report to : " + mmeteoBox.getCustomer().getName() );
			}
			else
			{
				logger.info("Generating report for mmeteoBox " + mmeteoBox.getImei() + " and customer " + mmeteoBox.getCustomer().getSocial() + " on " + mmeteoBox.getCustomer().getContactEmail() );
				
				DateTime now = new DateTime(DateTimeZone.UTC);
				
				String fileName = mmeteoBox.getImei() + "_" + now.toString("yyyy_MM_dd_HH_mm_ss_SSS") + "_" + UUID.randomUUID().toString() + ".pdf";
				String filePath = this._TempPath + fileName;
				
				String reportFilePath = null;

				HashMap<String, Object> params = new HashMap<String, Object>();
				
				if( hourInterval != null )
				{
					reportFilePath = this._ReportPath + "MeteoReportGroupByHours.jasper";
					params.put( "hourInterval", hourInterval );
				}
				else
				{
					reportFilePath = this._ReportPath + "MeteoReport.jasper";
				}
				
				params.put( "minDate", fromDate.toDate().getTime() );
				params.put( "maxDate", toDate.toDate().getTime()  );
				params.put( "imei", mmeteoBox.getImei() );
				params.put( "generationDate", new DateTime(DateTimeZone.UTC).toDate().getTime() );
				
				params.put(JRParameter.REPORT_LOCALE, new Locale( "fr", "FR" ) );
				
				final String finalTempFile = filePath;
			    final String finalReportFile = reportFilePath;
		        final HashMap<String, Object> finalParams = params;
		        
		        this._GenerationError = false;
		        
				session.doWork( new Work()
				{					
					@Override
	                public void execute(Connection connection) throws SQLException
	                {
	                    try
	                    {
	                        JasperPrint jasperPrint = JasperFillManager.fillReport( finalReportFile, finalParams, connection );
	        			    
	        			    JasperExportManager.exportReportToPdfFile( jasperPrint, finalTempFile );
	                    } 
	                    catch (Exception e)
	                    {
	                    	logger.error( e.getMessage() );
	                    	
	                    	ReportManager.this._GenerationError = true;
	                    }
	                }
	            });
				
				if( ReportManager.this._GenerationError ) 
				{
					sent = false;
				}
				else
				{
					String subject = "Votre rapport Monsieur Météo du " + fromDate.toDateTime(DateTimeZone.forID("Europe/Paris")).toString("dd/MM/yyyy HH:mm") + " au " + toDate.toDateTime(DateTimeZone.forID("Europe/Paris")).toString("dd/MM/yyyy HH:mm") + " Boitier Réf. : " + mmeteoBox.getReferenceNaio();
					
					mailController.sendMail( mmeteoBox.getCustomer().getContactEmail(), subject, "", fileName, filePath );
					
					sent = true;
				}
			}
		}
		catch(Exception e)
		{
			logger.error( e.getMessage() );
		}
		finally
		{
			
		}
		
		return sent;
	}
}
