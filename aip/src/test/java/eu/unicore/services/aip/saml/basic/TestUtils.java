package eu.unicore.services.aip.saml.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import eu.unicore.services.aip.saml.Utils;

/**
 * @author K. Benedyczak
 */
public class TestUtils 
{
	@Test
	public void testPreferredVoSelection()
	{
		String res = Utils.handlePreferredVo(new String[] {"/uudb/subvo"}, "/uudb", null);
		assertEquals("/uudb/subvo", res);

		res = Utils.handlePreferredVo(new String[] {}, "/uudb", null);
		assertNull(res);

		res = Utils.handlePreferredVo(null, "/uudb", null);
		assertNull(res);
		
		res = Utils.handlePreferredVo(new String[] {"/a", "/uudb/subvo"}, "/uudb", null);
		assertEquals("/uudb/subvo", res);

		res = Utils.handlePreferredVo(new String[] {"/a", "/uudb/subvo/subsub"}, "/uudb", null);
		assertEquals("/uudb/subvo/subsub", res);

		res = Utils.handlePreferredVo(new String[] {"/a", "/uudb/subvo/subsub", "/b"}, "/uudb", "/b");
		assertEquals("/uudb/subvo/subsub", res);

		res = Utils.handlePreferredVo(new String[] {"/a", "/uudb"}, "/uudb", "/a");
		assertNull(res);
	}
	
}
