/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.uas.security.vo.basic;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.uas.security.vo.VOCommonUtils;
import junit.framework.TestCase;

/**
 * @author K. Benedyczak
 */
public class TestVoUtils extends TestCase
{
	public void testPreferredVoSelection()
	{
		String res = VOCommonUtils.handlePreferredVo(new String[] {"/uudb/subvo"}, "/uudb", null);
		assertEquals("/uudb/subvo", res);

		res = VOCommonUtils.handlePreferredVo(new String[] {}, "/uudb", null);
		assertNull(res);

		res = VOCommonUtils.handlePreferredVo(null, "/uudb", null);
		assertNull(res);
		
		res = VOCommonUtils.handlePreferredVo(new String[] {"/a", "/uudb/subvo"}, "/uudb", null);
		assertEquals("/uudb/subvo", res);

		res = VOCommonUtils.handlePreferredVo(new String[] {"/a", "/uudb/subvo/subsub"}, "/uudb", null);
		assertEquals("/uudb/subvo/subsub", res);

		res = VOCommonUtils.handlePreferredVo(new String[] {"/a", "/uudb/subvo/subsub", "/b"}, "/uudb", "/b");
		assertEquals("/uudb/subvo/subsub", res);

		res = VOCommonUtils.handlePreferredVo(new String[] {"/a", "/uudb"}, "/uudb", "/a");
		assertNull(res);
	}
	
	public void testSplitByScope()
	{
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:Normal", "/a", "val1", "/b", "val2", "/c", "val3"));
		attrs.add(createScopedAttr("saml:Normal2", "/bb", "val1", "/aa", "val1", "/aa", "val2", "/bb", "val2"));
		
		List<ParsedAttribute> attrs2 = UVOSAttributeProfile.splitByScopes(attrs);
		assertEquals(5, attrs2.size());
	}
	
	public static ParsedAttribute createScopedAttr(String name, String... valsAndScopes)
	{
		ParsedAttribute ret = new ParsedAttribute(name);
		List<ScopedStringValue> scopedVals = new ArrayList<ScopedStringValue>(valsAndScopes.length/2);
		List<String> strVals = new ArrayList<String>(valsAndScopes.length/2);
		for (int i=0; i<valsAndScopes.length-1; i+=2)
		{
			scopedVals.add(new ScopedStringValue(valsAndScopes[i], 
					SAMLConstants.XACMLDT_STRING, valsAndScopes[i+1]));
			strVals.add(valsAndScopes[i+1]);
		}
		ret.setDataType(ScopedStringValue.class);
		ret.setValues(strVals, scopedVals);
		return ret;
	}
}
