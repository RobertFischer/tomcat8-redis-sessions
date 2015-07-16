package com.webonise.tomcat8.redisession;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.webonise.tomcat8.redisession.redisclient.Redis;

import redis.clients.jedis.Protocol;

public class RedisPoolFactory implements ObjectFactory {

	  public static final String DEFAULT_KEY_HOST = "host";
	  public static final String DEFAULT_KEY_PORT = "port";
	  public static final String DEFAULT_KEY_TIMEOUT =  "timeout";
	  private static Logger logger = Logger.getLogger(RedisPoolFactory.class.getName());
	
	  
	  /**
	   * Extracts properties from Resources
	   */
  public Object getObjectInstance(Object obj,
      Name name, Context nameCtx, Hashtable<?,?> environment)
      throws NamingException {

      // Acquire an instance of our specified bean class
      //
      final Map<String,Object> jedisConfig = new Hashtable<String,Object>();
      // Customize the bean properties from our attributes
      Reference ref = (Reference) obj;
      Enumeration addrs = ref.getAll();
      while (addrs.hasMoreElements()) {
          RefAddr addr = (RefAddr) addrs.nextElement();
          String propertyName = addr.getType();
          String propertyValue = (String) addr.getContent();
          
		 if (propertyName.equalsIgnoreCase(DEFAULT_KEY_HOST)){
			 jedisConfig.put(DEFAULT_KEY_HOST, propertyValue);
		 }
		 if (propertyName.equalsIgnoreCase(DEFAULT_KEY_PORT)){
			 jedisConfig.put(DEFAULT_KEY_PORT, propertyValue);
		 }
		 if (propertyName.equalsIgnoreCase(DEFAULT_KEY_TIMEOUT)){
			 jedisConfig.put(DEFAULT_KEY_TIMEOUT, propertyValue);
		 }
      }

      // Return the customized instance
      return (extractFromProperties(jedisConfig));

  }
  
  /**
   * Create an instance after silly validations
   * @param jedisConfig
   * @return
   */
  public Object extractFromProperties (final Map<String,Object> jedisConfig){
	  
	  		Redis redis = null;
	  		 String host = Protocol.DEFAULT_HOST ;
	  		 int port = Protocol.DEFAULT_PORT;
	  		 int timeout = Protocol.DEFAULT_TIMEOUT ;
	  		 try {
	  			if (Objects.nonNull(jedisConfig.get(DEFAULT_KEY_HOST))&&!jedisConfig.get(DEFAULT_KEY_HOST).toString().isEmpty()){
	  				host = jedisConfig.get(DEFAULT_KEY_HOST).toString();
	  			 }
	  			if (Objects.nonNull(jedisConfig.get(DEFAULT_KEY_PORT))&&!jedisConfig.get(DEFAULT_KEY_PORT).toString().isEmpty()){
	  				port = Integer.parseInt(jedisConfig.get(DEFAULT_KEY_PORT).toString());
	  			 }
	  			if (Objects.nonNull(jedisConfig.get(DEFAULT_KEY_TIMEOUT))&&!jedisConfig.get(DEFAULT_KEY_TIMEOUT).toString().isEmpty()){
	  				timeout = Integer.parseInt(jedisConfig.get(DEFAULT_KEY_TIMEOUT).toString());
	  			 }
	  			
	  	     redis = new Redis(host,port,timeout);
	  
			 } catch (Exception e) {
		            logger.log(Level.SEVERE, "There is one property with wrong description", e);
		        }
			
			 return redis ;
	}
  
}
