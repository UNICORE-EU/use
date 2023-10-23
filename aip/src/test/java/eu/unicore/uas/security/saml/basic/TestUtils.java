/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.uas.security.saml.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import eu.unicore.uas.security.saml.Utils;

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
