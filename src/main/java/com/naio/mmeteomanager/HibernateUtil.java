package com.naio.mmeteomanager;

import java.io.File;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import com.naio.mmeteopersistence.common.DatabaseInterceptor;
 
public class HibernateUtil
{
 	private static SessionFactory sessionFactory;
 	private static ServiceRegistry serviceRegistry;
 	private static DatabaseInterceptor databaseInterceptor;
 	private static Configuration configuration;
 	
	private static SessionFactory buildSessionFactory(String confFolderPath)
	{
		try 
		{
			configuration = new Configuration();

			databaseInterceptor = new DatabaseInterceptor();
 
			String hibernateCfgXmlFile = confFolderPath + "mmeteomanager.hibernate.cfg.xml";
			
			File cfgFile = new File(hibernateCfgXmlFile);
					
			configuration.configure( cfgFile );
			
			/*
			SchemaExport schema = new SchemaExport(configuration);
		    schema.setOutputFile("schema.sql");
		    schema.create(false, false);
			*/
			
			serviceRegistry = new StandardServiceRegistryBuilder().applySettings( configuration.getProperties() ).build();
			
			sessionFactory = configuration.setInterceptor( databaseInterceptor ).buildSessionFactory( serviceRegistry );
			
			return sessionFactory;
		}
		catch ( Throwable ex )
		{
			// Make sure you log the exception, as it might be swallowed
			System.err.println( "Initial SessionFactory creation failed." + ex );
			
			throw new ExceptionInInitializerError( ex );
		}
	}
 
	public static SessionFactory getSessionFactory(String confFolderPath)
	{
		if(  sessionFactory == null )
		{
			 sessionFactory = buildSessionFactory( confFolderPath );
		}
		
		return sessionFactory;
	}
 
	public static SessionFactory getSessionFactory()
	{
		return sessionFactory;
	}
	
	public static void shutdown() 
	{
		// Close caches and connection pools
		getSessionFactory().close();
	}
 
}