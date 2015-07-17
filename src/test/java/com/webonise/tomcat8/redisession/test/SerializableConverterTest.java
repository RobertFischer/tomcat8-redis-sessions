package com.webonise.tomcat8.redisession.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;


@RunWith(Parameterized.class)
public class SerializableConverterTest extends RedisConverterTest<Serializable> {

	
	 public String valueExpected = "" ;
		
	 public Integer valueInput = Integer.MIN_VALUE ; 
	 
	 SerializableConverter<Serializable> serConverter =  null;
	 
	
	 public SerializableConverterTest(String expected,Integer input){
		 valueExpected = expected;
		 valueInput = input;	 
		 serConverter =  new SerializableConverter<Serializable>();
	 }
	 
	 
	 @Test
		 public void testTwoObjects(){
		 
		  List<Integer>list1 =  new ArrayList<Integer>();
		  list1.add(2);
		  list1.add(3);
		  
		  List<Integer>list2 =  new ArrayList<Integer>();
		  list1.add(2);
		  list2.add(3);
		  
		  Assert.assertNotEquals(serConverter.convertToString(list2.toString()),serConverter.convertToString(list1.toString()));
		  
		 
	 };
		@Ignore
		@Override
		public void testDecode() {
			Assert.assertEquals(valueInput,serConverter.convertFromString(serConverter.convertToString(valueInput)));
		}
		
		@Ignore
		@Override
		public void testEncode() {
		  Assert.assertNotEquals(valueExpected,serConverter.convertToString(valueInput));
		}
		
		@Parameters
	    public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {     
	                 { "345",345 }, { "0",0 }, { "1444",1444 }   
	           });
	    }
}
