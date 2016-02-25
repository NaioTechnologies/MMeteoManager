package com.naio.util;

import java.util.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import org.apache.log4j.Logger;


public class GmailClient 
{
 	private static final Logger LOGGER = Logger.getLogger(GmailClient.class);
	
    private String userName;
    private String password;
    private String imapHost;
    //private int imapPort;
    private String sendingHost;
    private int sendingPort;
    private String auth;
    private String from;
    private String to;
    private String subject;
    private String text;
    private String receivingHost;
    //	private int receivingPort;
 
    public void initialize( String imapHost, int imapPort, String sendingHost, int sendingPort, String auth, String userName, String password )
    {
    	this.imapHost = imapHost;
    	//this.imapPort = imapPort;
        this.sendingHost = sendingHost;
        this.sendingPort = sendingPort;
        this.auth = auth;
    	this.userName=userName; //sender's email can also use as User Name
        this.password=password;
    }
 
    public void sendGmail( Mail mail, Boolean forwardInnerMessage )
    {
         // This will send mail from -->sender@gmail.com to -->receiver@gmail.com
        this.from = mail.getFrom().get(0);
        this.to = mail.getTo().get(0);
        this.subject = mail.getSubject();
        this.text = mail.getBody();
 
        // For a Gmail account--sending mails-- host and port shold be as follows
        // this.sendingHost="smtp.gmail.com";
        // this.sendingPort=465;
 
        Properties props = new Properties();
 
        props.put("mail.smtp.host", this.sendingHost);
        props.put("mail.smtp.port", String.valueOf(this.sendingPort));
        props.put("mail.smtp.user", this.userName);
        props.put("mail.smtp.password", this.password);
        props.put("mail.smtp.auth", this.auth);
 
        Session session1 = Session.getDefaultInstance(props);
 
        //MIME stands for Multipurpose Internet Mail Extensions
 
        InternetAddress fromAddress = null;
        InternetAddress toAddress = null;
 
        try
        {
            fromAddress = new InternetAddress(this.from);
            toAddress = new InternetAddress(this.to);
         }
        catch (AddressException e) 
        {
             e.printStackTrace();

             LOGGER.error("Sending email to: " + to + " failed !!!");
        }
 
        try 
        {
            if( forwardInnerMessage )
            {
            	Session forwardSession = Session.getDefaultInstance(props);
            	
            	// Create the message to forward
            	Message forward = new MimeMessage(forwardSession);

            	// Fill in header
            	forward.setSubject("Fwd: " + mail.getMessage().getSubject() );
            	forward.setFrom( new InternetAddress( this.from ) );
            
            	for( int iTo=0 ; iTo < mail.getTo().size() ; iTo++ )
            	{
            		forward.addRecipient( Message.RecipientType.TO, new InternetAddress( mail.getTo().get( iTo ) ) );
            	}
			
				// Create your new message part
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setText( "Original message:\n\n" );
				
				// Create a multi-part to combine the parts
				Multipart multipart = new MimeMultipart();
				multipart.addBodyPart( messageBodyPart );
				
				// Create and fill part for the forwarded content
				messageBodyPart = new MimeBodyPart();
				messageBodyPart.setDataHandler( mail.getMessage().getDataHandler() );
				
				// Add part to multi part
				multipart.addBodyPart( messageBodyPart );
				
				// Associate multi-part with message
				forward.setContent( multipart );
				
				// Send message
				Transport transport = session1.getTransport("smtps");
				  
				transport.connect( this.sendingHost, sendingPort, this.userName, this.password );
				  
				transport.sendMessage( forward, forward.getAllRecipients() );
				  
				transport.close();
				  
				LOGGER.info("Mail forward sent successfully ...");
            }
            else
            {
            	Message message = new MimeMessage(session1);
            	
            	message.setFrom(fromAddress);
          	  
            	//message.setRecipient(RecipientType.TO, toAddress);
            	
            	for( int iTo=0 ; iTo < mail.getTo().size() ; iTo++ )
            	{
            		message.addRecipient( Message.RecipientType.TO, new InternetAddress( mail.getTo().get( iTo ) ) );
            	}
            	            	
                // to add CC or BCC use
                // simpleMessage.setRecipient(RecipientType.CC, new InternetAddress("CC_Recipient@any_mail.com"));
                // simpleMessage.setRecipient(RecipientType.BCC, new InternetAddress("CBC_Recipient@any_mail.com"));
     
            	message.setSubject(this.subject);
            	
            	if( mail.getBody().startsWith("<") || mail.getBody().contains("<div") || mail.getBody().contains("<span")) 
            	{
            		message.setContent( this.text, "text/html" );
            	}
            	else
            	{
            		message.setText(this.text);
            	}
            	
            	if( mail.getAttachedFilePath() != null )
            	{
            		Multipart multipart = new MimeMultipart();
            		
            		MimeBodyPart messageBodyPart = new MimeBodyPart();
            		
            		DataSource source = new FileDataSource( mail.getAttachedFilePath() );
            	    messageBodyPart.setDataHandler( new DataHandler( source ) );
            	    messageBodyPart.setFileName( mail.getAttachedFileName() );
            	     
            	    multipart.addBodyPart(messageBodyPart);
            	    
            	    message.setContent( multipart );
            	}
            	
				Transport transport = session1.getTransport("smtps");
		            
				transport.connect (this.sendingHost,sendingPort, this.userName, this.password);
				
				transport.sendMessage(message, message.getAllRecipients());
				
				transport.close();
				
				LOGGER.info("Mail sent successfully : " + toAddress.getAddress() );
            }
        }
        catch (MessagingException e)
        {
             e.printStackTrace();
             
             LOGGER.error("Sending email to: " + to + " failed !!!" + toAddress.getAddress() );
        }
    }
 
    public List<Mail> readGmail( Boolean unseen, Boolean deleteReadMessage )
    {
    	List<Mail> mailList = new ArrayList<Mail>();
    	
        /*this will print subject of all messages in the inbox of sender@gmail.com*/
        this.receivingHost= this.imapHost;//for imap protocol
 
        Properties props2 = System.getProperties();
 
        props2.setProperty( "mail.store.protocol", "imaps" );
        // I used imaps protocol here
 
        Session session2 = Session.getDefaultInstance( props2 , null );
 
        try
        {
            Store store = session2.getStore("imaps");
 
            store.connect( this.receivingHost, this.userName, this.password );
 
            Folder folder=store.getFolder("INBOX");//get inbox
 
            folder.open(Folder.READ_WRITE);
                 
            Message message[] = null;
            
            if( unseen )
            {
            	FlagTerm flagTerm = new FlagTerm( new Flags( Flags.Flag.SEEN ), false );
            
            	message = folder.search(flagTerm);
            }
            else
            {
            	 message = folder.getMessages();
            }
 
            for( int i=0 ; i<message.length ; i++ )
            {
            	message[i].setFlag( Flags.Flag.SEEN, true );
 
                Mail mail = new Mail();

                mail.setMessage(message[i]);
                
                for( Address address : message[i].getFrom() )
                {
                	mail.getFrom().add( address.toString() );
                }

                mail.setSubject( message[i].getSubject() );
                
                String content = "";
                 
                if( message[i].getContent() instanceof Multipart )
                {
                	Multipart multipart = (Multipart) message[i].getContent();

                    for (int j = 0; j < multipart.getCount(); j++)
                    {
                        BodyPart bodyPart = multipart.getBodyPart(j);

                        String disposition = bodyPart.getDisposition();

                        if ( disposition != null && ( disposition.equalsIgnoreCase("ATTACHMENT") ) ) 
                        { 
                        	// BodyPart.ATTACHMENT doesn't work for gmail
                            //DataHandler handler = bodyPart.getDataHandler();
                        }
                        else
                        { 
                            content = bodyPart.getContent().toString();
                        }
                    }
                }
                else
                {
                	content = message[i].getContent().toString();
                }
                
                mail.setBody(content);
                
                /*
                if( p.isMimeType("multipart/*") ) {

                    Multipart mp = (Multipart)p.getContent();
                    // the content was not fetched from the server

                    // parse each Part
                    for (int i = 0; i < mp.getCount(); i++) {
                        Part inner_part = mp.getBodyPart(i)

                        if( inner_part.isMimeType("text/plain") ) {
                            String text = inner_part.getText();
                            // the content of this Part was fetched from the server
                        }
                    }
                }*/
                
                //mail.setBody(body);
                
                mailList.add(mail);
            }
 
            if( deleteReadMessage )
            {
            	for( int i=0 ; i<message.length ; i++ )
                {
            		message[i].setFlag( Flags.Flag.DELETED, true );
                }
            }
            
            //close connections

            folder.expunge();
            folder.close(false);
            
            store.close();
        }
        catch (Exception e)
        {
        	LOGGER.error(e.getMessage());
        }
        finally
        {
        	
        }
        
        return mailList;
    }
  
}