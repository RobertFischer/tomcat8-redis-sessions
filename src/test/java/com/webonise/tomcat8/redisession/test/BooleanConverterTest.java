package com.webonise.tomcat8.redisession.test;



import static org.junit.Assert.*;


import java.util.Arrays;
import java.util.Collection;







import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.BooleanConverter;
import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;



@RunWith(Parameterized.class)
public class BooleanConverterTest extends RedisConverterTest<Boolean>  {

	
	 public String valueExpected = "" ;

	 public Boolean valueInput = false ; 
	 
	 private SerializableConverter<Boolean> serConverter =  null;
	 
	 private  BooleanConverter bolConverter =  null;
	
	 public BooleanConverterTest(String expected, Boolean input) {
		 valueInput= input;
		 valueExpected= expected;
		 serConverter =  new SerializableConverter<Boolean>();
		 bolConverter =  new BooleanConverter();
	    }
	 
	

	@Test
	public void testEncode () { 
		
		assertNotEquals(valueExpected,serConverter.convertToString(valueInput));
		assertEquals(valueExpected,bolConverter.convertToString(valueInput));
	}
	

	@Test
	public void testDecode () {
		String temp =  serConverter.convertToString(valueInput); // convert into Base 64
		assertEquals(valueInput,serConverter.convertFromString(temp));
		}

	@Test
	 public void testTwoObjects(){
		/**
		 * This should fail if we want to encode the same value
		 */
		Boolean value1 =  true ;
		Boolean value2 = false ;
		assertNotEquals( serConverter.convertToString(value1), serConverter.convertToString(value2));
		
		
	};
	 
		
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "1",true }, { "0",false }, { "1",true }   
           });
    }



}
