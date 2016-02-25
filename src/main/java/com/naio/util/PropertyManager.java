package com.naio.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertyManager 
{
	private static final Logger LOGGER = Logger.getLogger(PropertyManager.class);
	
	public static final String MAIL_SMTP_HOST = "MAIL_SMTP_HOST";
	public static final String MAIL_SMTP_PORT = "MAIL_SMTP_PORT";
	public static final String MAIL_SMTP_USER = "MAIL_SMTP_USER";
	public static final String MAIL_SMTP_AUTH = "MAIL_SMTP_AUTH";
	public static final String MAIL_SMTP_PASSWORD = "MAIL_SMTP_PASSWORD";
	public static final String MAIL_IMAP_HOST = "MAIL_IMAP_HOST";
	public static final String MAIL_IMAP_PORT = "MAIL_IMAP_PORT";
	
	public static final String LOG_MAIL_ADDRESS = "LOG_MAIL_ADDRESS";

	public static final String SMS_USER_PASSWORD="SMS_USER_PASSWORD";
	public static final String SMS_USER_EMAIL="SMS_USER_EMAIL";
	public static final String SMS_TOKEN_URL="SMS_TOKEN_URL";
	public static final String SMS_HOST="SMS_HOST";
	public static final String SMS_SHORT_NAME="SMS_SHORT_NAME";

	public static final String ACTIVITY_MAX_DURATION_MINUTE = "ACTIVITY_MAX_DURATION_MINUTE";
	public static final String LAST_CHECK_MAX_DURATION_MINUTE = "LAST_CHECK_MAX_DURATION_MINUTE";
		
	Properties prop = new Properties();
	
	InputStream input = null;
		
	public PropertyManager()
	{
	}
	
	public void load(String confFolderPath)
	{
		try
		{
			this.input = new FileInputStream( confFolderPath + "mmeteomanager.properties" );
			
			this.prop = new Properties();

			this.prop.load(input);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		} 
		finally 
		{
			if (input != null) 
			{
				try 
				{
					input.close();
				}
				catch (IOException e) 
				{
					e.printStackTrace();
					
					LOGGER.error(e.getMessage());
				}
			}
		}
	}
	
	public String getValue(String key)
	{
		String value = null;
		
		value = this.prop.getProperty(key);
		
		if( value != null )
		{
			value = value.trim();
		}
		
		return value;
	}
	
}
