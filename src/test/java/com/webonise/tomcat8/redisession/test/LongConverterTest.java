package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.LongConverter;
import com.webonise.tomcat8.redisession.redisclient.SerializableConverter;


@RunWith(Parameterized.class)
public class LongConverterTest extends RedisConverterTest<Long> {

	

		 public String valueExpected = "" ;
	
		 public Long valueInput = Long.MIN_VALUE ; 
		 
		 private SerializableConverter<Long> serConverter =  null;
		 private LongConverter lgConverter =  null;
	
	public LongConverterTest(String expected,Long input) {
		 valueExpected = expected;
		 valueInput = input;
		 serConverter = new SerializableConverter<Long>();
		 lgConverter = new LongConverter();
	}
	
	@Test
	 public void testTwoObjects(){
		
		Long object1 = Long.MAX_VALUE;
		Long object2 = Long.MIN_VALUE;
		 Assert.assertNotEquals(serConverter.convertToString(object1),serConverter.convertToString(object2));
		 Assert.assertNotEquals(lgConverter.convertToString(object1),lgConverter.convertToString(object2));
	};
	 
	@Override
	public void testDecode() {
		 String encodelong = serConverter.convertToString(valueInput) ;		
		 assertEquals(valueInput, serConverter.convertFromString(encodelong));
		 assertEquals(valueInput, lgConverter.convertFromString(valueExpected));
	}
	
	@Override
	public void testEncode() {
	  assertEquals(valueExpected,lgConverter.convertToString(valueInput));
	  Assert.assertNotEquals(valueExpected,serConverter.convertToString(valueInput));
	}
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "345",345L }, { "0",0L }, { "1444",1444L }   
           });
    }
	
}
