package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;


import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


import com.webonise.tomcat8.redisession.redisclient.StringConverter;


@RunWith(Parameterized.class)
public class StringConverterTest extends RedisConverterTest<String> {
	
	public String expectedValue = null;
	public String inputValue =  null;
	
	public StringConverterTest(String input, String expected){
		 this.expectedValue =  expected ;
		 this.inputValue = input ;
	}
	
	@Override
	public void testDecode() {
		
		assertEquals(inputValue,new StringConverter().convertToString(inputValue));
	}
	
	@Override
	public void testEncode() {
		assertEquals(expectedValue,new StringConverter().convertFromString(inputValue));
	}
	
	
	
	
	@Override
	public void testTwoObjects() {
		
		super.testTwoObjects();
	}
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00","2015-07-13T11:17:38-06:00" }, { "2015-07-13T11:17:f51-06:00","2015-07-13T11:17:f51-06:00" }, { "2015-07-13T11fff:17:51-06:00","2015-07-13T11fff:17:51-06:00" }   
           });
    }

}
