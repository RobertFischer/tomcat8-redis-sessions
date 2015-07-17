package com.webonise.tomcat8.redisession.test;


import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;
import com.webonise.tomcat8.redisession.redisclient.TimestampConverter;


@RunWith(Parameterized.class)
public class TimestampConverterTest extends RedisConverterTest<Long> {

	public Long timestamp = 0L;
	public String value = "" ;
	
	 private SerializableConverter<Long> serConverter =  null;
	 private TimestampConverter timeConverter =  null;
	
	public TimestampConverterTest(String valueExpected , Long inputValue) {
		this.timestamp =  inputValue ;
		this.value = valueExpected ;
		 this.serConverter = new SerializableConverter<Long>();
		 this.timeConverter = new TimestampConverter(Date::new);
	}
	
	
    @Test
	@Ignore
	@Override
	public void testTwoObjects() {
		String timeStamp1= "2015-07-13T11:17:38-06:00" ;
		String timeStamp2= "2015-07-13T11:17:51-06:00" ;
		Assert.assertNotEquals(serConverter.convertToString(timeConverter.convertFromString(timeStamp2)),serConverter.convertToString(timeConverter.convertFromString(timeStamp1)));
		
	}
	
	
	@Override
	public void testDecode() {
		String encodeTme = serConverter.convertToString(timestamp);
		assertEquals(timestamp,timeConverter.convertFromString(value));
		assertEquals (timestamp,serConverter.convertFromString(encodeTme));
	}
	

	@Override
	public void testEncode() {
		assertEquals(value,timeConverter.convertToString(timestamp));
		Assert.assertNotEquals(value,serConverter.convertToString(timestamp));
	}
	
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00",1436807858000l }, { "2015-07-13T11:17:51-06:00",1436807871000l }, { "2015-07-13T11:17:51-06:00",1436807871000l }   
           });
    }
	
}
