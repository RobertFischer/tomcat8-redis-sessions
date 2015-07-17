package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;



import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;
import com.webonise.tomcat8.redisession.redisclient.StringConverter;


@RunWith(Parameterized.class)
public class StringConverterTest extends RedisConverterTest<String> {
	
	public String expectedValue = null;
	public String inputValue =  null;
	
	 private SerializableConverter<String> serConverter =  null;
	 private StringConverter strConverter = null;
	 
	public StringConverterTest(String input, String expected){
		 this.expectedValue =  expected ;
		 this.inputValue = input ;
		 serConverter =  new SerializableConverter<String>();
		 strConverter=  new  StringConverter();
	}
	
	@Override
	public void testDecode() {
		String inputEncode =  serConverter.convertToString(inputValue);
		assertEquals(expectedValue,strConverter.convertToString(serConverter.convertFromString(inputEncode)));
	}
	
	@Override
	public void testEncode() {
		Assert.assertNotEquals(expectedValue,strConverter.convertFromString(serConverter.convertToString(inputValue)));

	}
	
	
	
	
	@Override
	public void testTwoObjects() {
		
		String object1 = "String1" ;
		String objert2 = "String 2" ;
		
		Assert.assertNotEquals(serConverter.convertToString(object1),serConverter.convertToString(objert2));
	}
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "2015-07-13T11:17:38-06:00","2015-07-13T11:17:38-06:00" }, { "2015-07-13T11:17:f51-06:00","2015-07-13T11:17:f51-06:00" }, { "2015-07-13T11fff:17:51-06:00","2015-07-13T11fff:17:51-06:00" }   
           });
    }

}
