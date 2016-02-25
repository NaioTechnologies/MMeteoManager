package com.naio.mmeteomanager;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;






import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;

import com.naio.mmeteopersistence.entity.Naio;
import com.naio.util.GmailClient;
import com.naio.util.Mail;
import com.naio.util.PropertyManager;

public class MailController
{
	private static final Logger LOGGER = Logger.getLogger(MailController.class);
	
	GmailClient _GmailClient;
	PropertyManager _PropertyManager;
	
	public MailController(PropertyManager propertyManager)
	{
		this._PropertyManager = propertyManager;
		
		this._GmailClient = new GmailClient();
		
		this._GmailClient.initialize( 
									  this._PropertyManager.getValue(PropertyManager.MAIL_IMAP_HOST),
									  Integer.parseInt(this._PropertyManager.getValue(PropertyManager.MAIL_IMAP_PORT)),
									  this._PropertyManager.getValue(PropertyManager.MAIL_SMTP_HOST),
									  Integer.parseInt(this._PropertyManager.getValue(PropertyManager.MAIL_SMTP_PORT)),
									  this._PropertyManager.getValue(PropertyManager.MAIL_SMTP_AUTH),
									  this._PropertyManager.getValue(PropertyManager.MAIL_SMTP_USER),
									  this._PropertyManager.getValue(PropertyManager.MAIL_SMTP_PASSWORD) 
									 );
	}
	
	public List<Mail> readAndForwardReceivedMail()
	{
		List<Mail> mailList = new ArrayList<Mail>();
		
		mailList = this._GmailClient.readGmail(true, false);
		
		for( Mail mail : mailList )
		{
			mail.getTo().clear();
			
			mail.getTo().add( this._PropertyManager.getValue( PropertyManager.LOG_MAIL_ADDRESS ) );

			mail.setSubject( "Fwd: " + mail.getSubject() );
			
			this._GmailClient.sendGmail( mail, false );
		}
		
		return mailList;
	}
	
	@SuppressWarnings("unchecked")
	public void mailLog( Session session, String level, String subject, String message )
	{
		try
		{
			Criteria naioCriteria = session.createCriteria(Naio.class);
			
			List<Naio> naioList = (List<Naio>)(naioCriteria.list());
			
			if( naioList.size() == 0 )
			{
				LOGGER.warn("No Naio user set as SendServerError, sending to emergency email.");
				
				this.mailLog(level, subject, message);
			}
			else
			{
				for( Naio naio : naioList )
				{
					if( naio.getSendServerError() )
					{
						Mail mail = new Mail();
						
						mail.getTo().add( naio.getEmail() );
						
						mail.getFrom().add( this._PropertyManager.getValue( PropertyManager.MAIL_SMTP_USER ) );
						
						mail.setSubject( level + " : " + subject );
						
						mail.setBody( message );
						
						this._GmailClient.sendGmail(mail, false);
					}
				}
			}
		}
		catch(Exception e)
		{
			LOGGER.error("mailLog problem, sending to emergency email." );
			LOGGER.error( e.getMessage() );
			
			this.mailLog(level, subject, message);
		}
	}
	
	public void mailLog( String level, String subject, String message )
	{
		Mail mail = new Mail();
		
		String[] tos = this._PropertyManager.getValue( PropertyManager.LOG_MAIL_ADDRESS ).split(";");
		
		for(int i=0;i<tos.length;i++)
		{
			mail.getTo().add( tos[i] );	
		}
		
		mail.getFrom().add( this._PropertyManager.getValue( PropertyManager.MAIL_SMTP_USER ) );
		
		mail.setSubject( level + " : " + subject );
		
		mail.setBody( message );
		
		this._GmailClient.sendGmail(mail, false);
	}
	
	public void sendMail( String to, String subject, String message )
	{
		Mail mail = new Mail();
		
		String[] tos = to.split(";");
		
		for(int i=0;i<tos.length;i++)
		{
			mail.getTo().add( tos[i] );	
		}
		
		mail.getFrom().add( this._PropertyManager.getValue( PropertyManager.MAIL_SMTP_USER ) );
		
		mail.setSubject( subject );

		mail.setBody( message );
		
		this._GmailClient.sendGmail(mail, false);
	}
	
	public void sendMail( String to, String subject, String message, String fileName, String filePath )
	{
		Mail mail = new Mail();
		
		String[] tos = to.split(";");
		
		for(int i=0;i<tos.length;i++)
		{
			mail.getTo().add( tos[i] );	
		}
		
		mail.getFrom().add( this._PropertyManager.getValue( PropertyManager.MAIL_SMTP_USER ) );
		
		mail.setSubject( subject );

		mail.setBody( message );
		
		mail.setAttachedFileName(fileName);
		
		mail.setAttachedFilePath(filePath);
		
		this._GmailClient.sendGmail(mail, false);
	}
}
