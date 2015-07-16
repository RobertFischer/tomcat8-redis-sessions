package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.DateConverter;



@RunWith(Parameterized.class)
public class DateConverterTest extends RedisConverterTest<String> {
	
	
	public Date inputvalue = null;
	public String expectedValue = null;
	
	public DateConverterTest(String expectedValue, Date inputValue) {
		this.inputvalue = inputValue;
		this.expectedValue = expectedValue ;
	}
	
	@Override
	public void testDecode() {
		assertEquals(new DateConverter(Date::new).convertToString(inputvalue),new DateConverter(Date::new).convertFromString(expectedValue));
	}
	
	@Override
	public void testTwoObjects() {
		// TODO Auto-generated method stub
		super.testTwoObjects();
	}
	
	@Override
	public void testEncode() {
		System.out.println(new DateConverter(Date::new).convertFromString("2015-07-15-06:00"));
	}
	
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00",new Date() }, { "2015-07-13T11:17:51-06:00",new Date() }, { "2015-07-13T11:17:51-06:00",new Date() }   
           });
    }

}
