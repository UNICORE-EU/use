package eu.unicore.services.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.xmlbeans.XmlObject;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.util.AuthZAttributeStore;

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
		List<String>ids = new ArrayList<>();
		for(int i=0; i<10000; i++) {
			String s = Utilities.newUniqueID();
			assertTrue(s.length()>8);
			assertFalse(ids.contains(s));
			ids.add(s);
		}
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
		CircuitBreaker cb = new CircuitBreaker(200);
		cb.notOK();
		assertEquals(false,cb.isOK());
		Thread.sleep(100);
		assertEquals(false,cb.isOK());
		Thread.sleep(2000);
		assertEquals(true,cb.isOK());
	}
	
	@Test
	public void testTimeProfile()throws Exception {
		TimeProfile tp = AuthZAttributeStore.getTimeProfile();
		tp.time("something");
		Thread.sleep(100);
		tp.time("some_other_thing");
		System.out.println(tp.toString());
	}

}
