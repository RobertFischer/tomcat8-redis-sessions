package com.webonise.tomcat8.redisession.test;


import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.TimestampConverter;


@RunWith(Parameterized.class)
public class TimestampConverterTest extends RedisConverterTest<Long> {

	public Long timestamp = 0L;
	public String value = "" ;
	
	public TimestampConverterTest(String valueExpected , Long inputValue) {
		this.timestamp =  inputValue ;
		this.value = valueExpected ;
	}
	
	
    @Test
	@Ignore
	@Override
	public void testTwoObjects() {
		// TODO Auto-generated method stub
		super.testTwoObjects();
	}
	
	
	@Override
	public void testDecode() {
		assertEquals(timestamp,new TimestampConverter(Date::new).convertFromString(value));
	}
	

	@Override
	public void testEncode() {
		assertEquals(value,new TimestampConverter(Date::new).convertToString(timestamp));
	}
	
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00",1436807858822L }, { "2015-07-13T11:17:51-06:00",1436807871966L }, { "2015-07-13T11:17:51-06:00",1436807871966L }   
           });
    }
	
}
