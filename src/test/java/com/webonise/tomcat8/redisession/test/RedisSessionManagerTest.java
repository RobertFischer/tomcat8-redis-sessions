package com.webonise.tomcat8.redisession.test;






import javax.naming.NamingException;

import org.apache.catalina.Session;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.webonise.tomcat8.redisession.RedisSession;
import com.webonise.tomcat8.redisession.RedisSessionManager;
import com.webonise.tomcat8.redisession.redisclient.Redis;


public class RedisSessionManagerTest   {

	 private static RedisSessionManager redisSessionManagerT8 = null;
	 private RedisSession session = null;
	 private Redis redis = null;
	 
	 @BeforeClass
	    public static void start() throws NamingException{
	        redisSessionManagerT8 = new RedisSessionManager();	
	        redisSessionManagerT8.setMaxInactiveInterval(1300);
	        redisSessionManagerT8.setSessionMaxAliveTime(100);
	       
	 }
	 
	 @Ignore
	 @Test
	    public void testSessionSetAtributes() throws Exception {
	        RedisSession tempSession = redisSessionManagerT8.createSession("newId2016");     
	        Assert.assertNotNull(tempSession);
	 	
	    }
	 
	 @Ignore
	 @Test
	    public void testAddSession() throws Exception {
		 RedisSession tempSession =  new RedisSession(redisSessionManagerT8,"addedSession");	 
	        Assert.assertNotNull(tempSession);
	    }
	 
	
	 
	 
	 @Test
	    public void testCreateSession() throws Exception {
		 
		
	        session = redisSessionManagerT8.createSession("newGeneratedId0123445678901");
	        Assert.assertNotNull(session);
	     
	    }
	 
	 @Ignore
	 @Test
	    public void testChangeAttributes() throws Exception {
		 	Assert.assertNotNull(session);
	        session.setAttribute("NewAttribute", "zdsdssdssd");
	       
	    
	    }
	 
	 @Ignore
	 @Test
	    public void testRemoveSession() throws Exception {
		   RedisSession tempSession = redisSessionManagerT8.createSession("newId2015");
		 	Assert.assertNotNull(tempSession);
		 	redisSessionManagerT8.remove(tempSession);
	       
	    
	    }
	 
	 @Ignore
	 @Test
	    public void testFindSessions() throws Exception {
		 Session[] sessions = null;
		 sessions = redisSessionManagerT8.findSessions();
	        Assert.assertNotNull(sessions);
	      
	    }
	 
	 
	 @Ignore
	 @Test
	    public void testGetRedis() throws Exception {
	 
	        Assert.assertNotNull(redisSessionManagerT8.getRedis());
	        
	    }


@Ignore
	 @Test
	    public void testChangeIdSession() throws Exception {
		 RedisSession sessionFound = redisSessionManagerT8.findSession("newGeneratedId1234456789");
		    Assert.assertNotNull(sessionFound);
	       sessionFound.setMaxInactiveInterval(3600);
	    }
	 
	 @Ignore
	 @Test
	    public void testChangeIdSessionNoParam() throws Exception {
		 RedisSession sessionFound = redisSessionManagerT8.findSession("newId201");
		    Assert.assertNotNull(sessionFound);
	        redisSessionManagerT8.changeSessionId(sessionFound);      
	    }
	 
	 
	 @Ignore
	 @Test
	    public void testFindSession() throws Exception {
	        RedisSession sessionFound = redisSessionManagerT8.findSession("newGeneratedId");
	        Assert.assertNotNull(sessionFound);
	       
	    }

	 
	 @Ignore
	 @Test
	    public void testRemovedSession() throws Exception {
	       RedisSession sessionFound = redisSessionManagerT8.findSession("addedSession");
	        redisSessionManagerT8.remove(sessionFound);
	       Assert.assertTrue(sessionFound.isValid()); 
	  
	    }
}
