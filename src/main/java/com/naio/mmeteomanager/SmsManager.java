package com.naio.mmeteomanager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import com.naio.util.PropertyManager;

public class SmsManager
{
	private static final Logger LOGGER = Logger.getLogger(SmsManager.class);
	
	PropertyManager _PropertyManager;
	String _LastTokenData;
	
	String _LastUserId;
	String _LastTimestamp;
	String _LastHash;
	
	public SmsManager(PropertyManager propertyManager)
	{
		this._PropertyManager = propertyManager;
	}
	
	public Boolean authenticate() throws Exception
	{
		Boolean authenticated = false;
		
		HttpURLConnection conn = null;
		
		try
		{
			URL url = new URL( this._PropertyManager.getValue( PropertyManager.SMS_TOKEN_URL ) );
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			
			// CHANGE EMAIL AND PASSWORD HERE
	        conn.setRequestProperty("email", this._PropertyManager.getValue( PropertyManager.SMS_USER_EMAIL ) );
	        conn.setRequestProperty("password", this._PropertyManager.getValue( PropertyManager.SMS_USER_PASSWORD ) );
	        
			if (conn.getResponseCode() != 200)
			{
				LOGGER.error( "authenticate : connection failed : HTTP error code : " + conn.getResponseCode() );
				
				throw new RuntimeException("authenticate : connection failed : HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader( new InputStreamReader( ( conn.getInputStream() ) ) );

			String output;
			
			while ((output = br.readLine()) != null)
			{
				//{"userId":"538479ede4b0c529c5851e24","timestamp":"1401193122336","hash":"5Enpnb9KZ8PT/fCriFnn2BKd8p8="}
				if( output.contains("userId") )
				{
					_LastTokenData = output;
					
					String tokenLine = output;
					
					tokenLine = tokenLine.replace("{", "");
					tokenLine = tokenLine.replace("}", "");
										
					tokenLine = tokenLine.replace("userId", "");
					tokenLine = tokenLine.replace("timestamp", "");
					tokenLine = tokenLine.replace("hash", "");
					
					tokenLine = tokenLine.replace("\"", "");
					tokenLine = tokenLine.replace(":", "");
					
					String[] elements = tokenLine.split(",");
					
					if( elements.length != 3 )
					{
						LOGGER.error( "authenticate : Bad token line received : " + tokenLine );
						
						throw new RuntimeException( "authenticate : Bad token line received : " + tokenLine );
					}
					else
					{
						this._LastUserId = elements[0];
						this._LastTimestamp = elements[1];
						this._LastHash = elements[2];
					}
				}
			}
			
			conn.disconnect();
			
			authenticated = true;
		} 
		catch (Exception e)
		{
			if( conn != null )
			{
				conn.disconnect();	
			}
			
			LOGGER.error( "authenticate error : " + e.getMessage() );
			
			throw new Exception( "authenticate error : " + e.getMessage(), e.getCause() );
		}
		
		return authenticated;
	}
	
	
	public void sendSms(final String remoteTel, final String message)
	{
		HttpURLConnection conn = null;
		
		String parsedMessage = message.replace("\n", "\\n" );
		
		try 
		{
			URL url = new URL( this._PropertyManager.getValue( PropertyManager.SMS_HOST ) );
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setDoOutput(true);
			
			conn.setRequestMethod("PUT");
	        conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
            
			conn.setRequestProperty("userId",  this._LastUserId );
            conn.setRequestProperty("timestamp", this._LastTimestamp );
            conn.setRequestProperty("hash", this._LastHash );
	        
	        String input = "{\"number\":\""+ remoteTel + "\",\"message\":\"" + parsedMessage + "\",\"sender\":\"" + this._PropertyManager.getValue( PropertyManager.SMS_SHORT_NAME ) + "\"}";
				        
	        OutputStream os = conn.getOutputStream();
	        
			os.write( input.getBytes() );
			
			os.flush();

			int responseCode = conn.getResponseCode();

			if ( responseCode != 200 )
			{
				throw new RuntimeException("sendSms Failed : " + remoteTel + " HTTP error code : " + conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader( ( conn.getInputStream() ) ) );

			String output;
			
			while ((output = br.readLine()) != null)
			{
				LOGGER.info("sendSms : " + output );
			}

			LOGGER.info("SMS sent successfully : " + remoteTel );
	  } 
	  catch (Exception e)
	  {
		  if( conn != null )
		  {
			  conn.disconnect();
		  }
		  
		  throw new RuntimeException("sendSms Failed : " + remoteTel + " " + e.getMessage());
	  }
	}
}
