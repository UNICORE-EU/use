package de.fzj.unicore.wsrflite.utils;

import static org.junit.Assert.*;

import java.util.Properties;

import org.apache.xmlbeans.XmlObject;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.wsrflite.ContainerProperties;

public class TestUtilities {
	
	static String gw="http://gwhost:1234";
	static ContainerProperties k;
	
	@BeforeClass
	public static void setUp(){
		Properties properties = new Properties();
		String base=gw+"/SITE";
		k=new ContainerProperties(properties, false);
		k.setProperty(ContainerProperties.EXTERNAL_URL, base);
		k.setProperty(ContainerProperties.SERVER_HOST, "my");
		k.setProperty(ContainerProperties.SERVER_PORT, "5678");
	}
	
	@Test
	public void testNewUniqueID() {
		assertTrue(Utilities.newUniqueID().length()>8);
	}

	@Test
	public void testExtractServiceName() {
		assertEquals("test",Utilities.extractServiceName("http://gw:1234/site/test?res=x"));
	}

	@Test
	public void testExtractElementTextAsString()throws Exception{
		String expected="text content";
		XmlObject source=XmlObject.Factory.parse("<x xmlns=\"n\">"+
				expected +
				"</x>");
		String found=Utilities.extractElementTextAsString(source);
		assertEquals(expected,found);
	}
	
	@Test
	public void testValidateIntegerRange() {
		assertTrue(Utilities.validateIntegerRange("10", 1, 10));
		assertTrue(Utilities.validateIntegerRange("1", 1, 10));
		assertFalse(Utilities.validateIntegerRange("100", 1, 10));
		//null values are validated as false
		assertFalse(Utilities.validateIntegerRange(null, 1, 10));
	}

	@Test
	public void testGetPhysicalServerAddress() {
		String found=Utilities.getPhysicalServerAddress(k, false);
		String host="http://my:5678";
		assertEquals(host,found);
	}

	@Test
	public void testGetGatewayAddress()throws Exception{
		String found=Utilities.getGatewayAddress(k);
		assertEquals(gw,found);
	}
	
	@Test
	public void testCircuitBreaker() throws Exception {
		CircuitBreaker cb = new CircuitBreaker("test",200);
		cb.notOK("some error occured");
		assertEquals(false,cb.isOK());
		assertTrue(cb.getValue().contains("some error"));
		Thread.sleep(100);
		assertEquals(false,cb.isOK());
		assertTrue(cb.getValue().contains("some error"));
		Thread.sleep(2000);
		assertEquals(true,cb.isOK());
		assertFalse(cb.getValue().contains("some error"));
		assertTrue(cb.getName().contains("test"));
	}

}
