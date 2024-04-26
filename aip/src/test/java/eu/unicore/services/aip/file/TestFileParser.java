package eu.unicore.services.aip.file;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author golbi
 */
public class TestFileParser {
	private static final String WRONG[] = {
			"", 
			"<dsdfsdf/>",
			"<fileAttributeSource><aaa/></fileAttributeSource>",
			"<fileAttributeSource><entry key=\"\"><aaa/></entry></fileAttributeSource>",
			"<fileAttributeSource><entry></entry></fileAttributeSource>",
			"<fileAttributeSource><entry key=\"\"><attribute name=\"\"><aaa/>" +
					"</attribute></entry></fileAttributeSource>",
					"<fileAttributeSource><entry key=\"\">" +
	"<attribute><aaa/></attribute></entry></fileAttributeSource>"};

	private static final String OK = 
			"<?xml version=\"1.0\" encoding=\"UTF-16\"?><fileAttributeSource>" +
					"   <entry key=\"CN=Stanisław Lem, C=PL\">" +
					"      <attribute name=\"role\"><value>user</value></attribute>" +
					"      <attribute name=\"empty\"/>" +
					"      <attribute name=\"xlogin\">" +
					"         <value>somebody</value>" +
					"         <value>nobody</value>" +
					"      </attribute>" +			
					"   </entry>" +
					"   <entry key=\"CN=Dead Man, C=US\">" +
					"      <attribute name=\"role\"><value>user</value></attribute>" +
					"   </entry>" +
					"</fileAttributeSource>";
	private static final String OK2 = 
			"<fileAttributeSource/>";

	private static final String OK3 = 
			"<?xml version=\"1.0\" encoding=\"UTF-16\"?><fileAttributeSource>" +
					"   <entry>"+
					"      <key>CN=Stanisław Lem, C=PL</key>" +
					"      <attribute name=\"role\"><value>user</value></attribute>" +
					"      <attribute name=\"empty\"/>" +
					"      <attribute name=\"xlogin\">" +
					"         <value>somebody</value>" +
					"         <value>nobody</value>" +
					"      </attribute>" +			
					"   </entry>" +
					"   <entry key=\"CN=Dead Man, C=US\">" +
					"      <attribute name=\"role\"><value>user</value></attribute>" +
					"   </entry>" +
					"</fileAttributeSource>";

	@Test
	public void testWrong() throws UnsupportedEncodingException
	{
		for (String toTest: WRONG)
		{
			try
			{
				new XMLFileParser().parse(new ByteArrayInputStream(toTest.getBytes("UTF-16")));
				fail("Invalid XML was parsed correctly: " + toTest);
			} catch (IOException e){}
		}
	}

	@Test
	public void testOK2() throws Exception
	{
		Map<String, List<Attribute>> map = new XMLFileParser().
				parse(new ByteArrayInputStream(OK2.getBytes("UTF-16")));
		assertTrue(map.isEmpty());
	}

	@Test
	public void testOK() throws Exception {
		Map<String, List<Attribute>> map = new XMLFileParser().
				parse(new ByteArrayInputStream(OK.getBytes("UTF-16")));
		
		assertTrue(map.size() == 2);
		List<Attribute> attrs = map.get("CN=Stanisław Lem, C=PL");
		assertTrue(attrs != null && attrs.size() == 3);
		assertTrue(attrs.get(0).getName().equals("role") &&
				attrs.get(0).getValues().size() == 1 &&
				attrs.get(0).getValues().get(0).equals("user"));
		assertTrue(attrs.get(1).getName().equals("empty") &&
				attrs.get(1).getValues().size() == 0);
		assertTrue(attrs.get(2).getName().equals("xlogin") &&
				attrs.get(2).getValues().size() == 2 &&
				attrs.get(2).getValues().get(0).equals("somebody") &&
				attrs.get(2).getValues().get(1).equals("nobody"));

		attrs = map.get("CN=Dead Man, C=US");
		assertTrue(attrs != null && attrs.size() == 1);
		assertTrue(attrs.get(0).getName().equals("role") &&
				attrs.get(0).getValues().size() == 1 &&
				attrs.get(0).getValues().get(0).equals("user"));
	}

	@Test
	public void testOK3() throws Exception {
		Map<String, List<Attribute>> map = new XMLFileParser().
				parse(new ByteArrayInputStream(OK3.getBytes("UTF-16")));
		List<Attribute> attrs = map.get("CN=Stanisław Lem, C=PL");
		assertTrue(attrs != null && attrs.size() == 3);
	}
}
