package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.DateConverter;
import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;



@RunWith(Parameterized.class)
public class DateConverterTest extends RedisConverterTest<Date> {
	
	
	public Date inputvalue = null;
	public String expectedValue = null;
	 private SerializableConverter<Date> serConverter =  null;
	 private DateConverter dateConverter = null;
	
	public DateConverterTest(String expectedValue, Date inputValue) {
		this.inputvalue = inputValue;
		this.expectedValue = expectedValue ;
		 serConverter =  new SerializableConverter<Date>();
		 dateConverter =  new DateConverter(Date::new);
	}
	
	@Override
	public void testDecode() {
		String encodeDate =  serConverter.convertToString(inputvalue);
		assertEquals(inputvalue,dateConverter.convertFromString(expectedValue));
		assertEquals(inputvalue,serConverter.convertFromString(encodeDate));
	}
	
	@Override
	public void testTwoObjects() {
		Date date1 =  new Date ();
		Date date2 = new Date(1437147065579l);
		Assert.assertNotEquals(serConverter.convertToString(date1),serConverter.convertToString(date2));
		Assert.assertNotEquals(dateConverter.convertToString(date1),dateConverter.convertToString(date2));
	}
	
	@Override
	public void testEncode() {
		assertEquals(expectedValue,dateConverter.convertToString(inputvalue));
		Assert.assertNotEquals(expectedValue,serConverter.convertToString(inputvalue));
	}
	
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00" ,new Date(1436807858000l) }, { "2015-07-13T11:17:51-06:00" ,new Date(1436807871000l) }, {"2015-07-13T11:17:51-06:00" ,new Date(1436807871000l) }   
           });
    }

}
