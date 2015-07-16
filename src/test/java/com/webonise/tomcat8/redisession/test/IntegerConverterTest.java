package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.IntegerConverter;

@RunWith(Parameterized.class)
public class IntegerConverterTest extends RedisConverterTest<String> {
	
	 public String valueExpected = "" ;
		
	 public Integer valueInput = Integer.MIN_VALUE ; 
	 
	 public IntegerConverterTest(String expected,Integer input){
		 valueExpected = expected;
		 valueInput = input;	 
	 }
	 
	    @Test
		@Ignore
		 public void testTwoObjects(){};
		 
		@Override
		public void testDecode() {
			 assertEquals(valueInput,new IntegerConverter().convertFromString(valueExpected));
		}
		
		@Override
		public void testEncode() {
		  assertEquals(valueExpected,new IntegerConverter().convertToString(valueInput));
		}
		
		@Parameters
	    public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {     
	                 { "345",345 }, { "0",0 }, { "1444",1444 }   
	           });
	    }
}
