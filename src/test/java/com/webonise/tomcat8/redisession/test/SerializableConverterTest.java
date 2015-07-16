package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.IntegerConverter;
import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;


@RunWith(Parameterized.class)
public class SerializableConverterTest extends RedisConverterTest<Serializable> {

	
	 public String valueExpected = "" ;
		
	 public Integer valueInput = Integer.MIN_VALUE ; 
	 
	
	 public SerializableConverterTest(String expected,Integer input){
		 valueExpected = expected;
		 valueInput = input;	 
	 }
	 
	 @Test
		@Ignore
		 public void testTwoObjects(){};
		 
		@Override
		public void testDecode() {
			Assert.assertNotEquals(valueInput,new SerializableConverter<>().convertFromString(valueExpected));
		}
		
		@Override
		public void testEncode() {
		  Assert.assertNotEquals(valueExpected,new SerializableConverter<>().convertToString(valueInput));
		}
		
		@Parameters
	    public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {     
	                 { "345",345 }, { "0",0 }, { "1444",1444 }   
	           });
	    }
}
