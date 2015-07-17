package com.webonise.tomcat8.redisession.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.JedisPool;
import junit.framework.TestCase;

public class JNDITestCases extends TestCase {
	
	private InitialContext ic = null;
	
	@BeforeClass
	public void setUp() throws Exception {
        // rcarver - setup the jndi context and the datasource
        try {
            // Create initial context
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
            ic = new InitialContext();
            ic.createSubcontext("java:");
            ic.createSubcontext("java:/comp");
            ic.createSubcontext("java:/comp/env/pool");
           
            JedisPool pool = new JedisPool();

            //JedisPool bean = (JedisPool) initCtx.lookup("java:comp/env/pool/JedisPool");
  	      
           ic.bind("java:/comp/env/pool/JedisPool", pool);
        } catch (NamingException ex) {
            Logger.getLogger(JNDITestCases.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
	@Test
	public void testConnection(){
		
		Context webContext = null;
	    JedisPool ds = null;
		try {
			webContext  = (Context)ic.lookup("java:/comp/env/pool");
			ds  = (JedisPool) webContext.lookup("pool/JedisPool");
	
			Assert.assertNotNull(ds);
	       
		} catch (NamingException e) {
			
		}
       

		
	}

}
