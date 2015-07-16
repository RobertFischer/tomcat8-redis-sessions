package com.webonise.tomcat8.redisession.test;



import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;


import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.BooleanConverter;



@RunWith(Parameterized.class)
public class BooleanConverterTest extends RedisConverterTest<Boolean>  {


	 public String valueExpected = "" ;

	 public Boolean valueInput = false ; 
	
	 public BooleanConverterTest(String expected, Boolean input) {
		 valueInput= input;
		 valueExpected= expected;
	    }
	 
	@Ignore
	@Test
	public void testEncode () { 
		
		assertEquals(valueExpected,new BooleanConverter().convertToString(valueInput));
	}
	
	@Ignore
	@Test
	public void testDecode () {
		assertEquals(valueInput,new BooleanConverter().convertFromString(valueExpected));
		}

	@Test
	@Ignore
	 public void testTwoObjects(){};
	 
		
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "1",true }, { "0",false }, { "1",true }   
           });
    }



}
