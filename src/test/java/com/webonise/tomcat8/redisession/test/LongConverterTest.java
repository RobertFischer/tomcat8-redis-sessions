package com.webonise.tomcat8.redisession.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.webonise.tomcat8.redisession.redisclient.LongConverter;


@RunWith(Parameterized.class)
public class LongConverterTest extends RedisConverterTest<Long> {

	

		 public String valueExpected = "" ;
	
		 public Long valueInput = Long.MIN_VALUE ; 
	
	public LongConverterTest(String expected,Long input) {
		 valueExpected = expected;
		 valueInput = input;
	}
	
	@Test
	@Ignore
	 public void testTwoObjects(){};
	 
	@Override
	public void testDecode() {
		 assertEquals(valueInput,new LongConverter().convertFromString(valueExpected));
	}
	
	@Override
	public void testEncode() {
	  assertEquals(valueExpected,new LongConverter().convertToString(valueInput));
	}
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 { "345",345L }, { "0",0L }, { "1444",1444L }   
           });
    }
	
}
