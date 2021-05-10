/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 16, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.attrprofile.UVOSAttributeProfile.ScopedStringValue;
import eu.unicore.uas.security.vo.UnicoreAttributeMappingDef;
import eu.unicore.uas.security.vo.UnicoreAttributesHandler;
import eu.unicore.uas.security.vo.UnicoreIncarnationAttributes;
import eu.unicore.uas.security.vo.VOCommonUtils;
import eu.unicore.uas.security.vo.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;


public class UnicoreAttributesHandlerTest 
{

	@Test
	public void testEmpty()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("xlogin", "/uudb", "val1"));
		
		UnicoreIncarnationAttributes result = testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().isEmpty());
		assertTrue(result.getValidAttributes().isEmpty());
	}

	/**
	 * Tests default and valid values. defAttr2 is disabled in configuration.
	 */
	@Test
	public void testDefaultAndDisabled()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
			new UnicoreAttributeMappingDef("defAttr", true, true),
			new UnicoreAttributeMappingDef("defAttr2", true, true),
			new UnicoreAttributeMappingDef("defAttr3", true, true)
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:b1", "/uudb", "val1", "val2"));
		attrs.add(createScopedAttr("saml:b1Def", "/uudb", "zzz"));
		attrs.add(createScopedAttr("saml:b2", "/uudb", "val1", "val2"));
		attrs.add(createScopedAttr("saml:b2Def", "/uudb", "zzz"));
		
		UnicoreIncarnationAttributes result =  testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().size() == 1 && 
				result.getDefaultAttributes().get("defAttr") != null &&
				result.getDefaultAttributes().get("defAttr")[0].equals("zzz"));
		assertTrue(result.getValidAttributes().size() == 1 && 
				result.getValidAttributes().get("defAttr") != null &&
				result.getValidAttributes().get("defAttr").length == 3);
		
		attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:b3Def", "/uudb", "zzz"));
		result = testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().size() == 1);
		assertTrue(result.getValidAttributes().size() == 1);
	}

	/**
	 * Tests if attribute in wrong scope won't be added
	 */
	@Test
	public void testScope()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
				new UnicoreAttributeMappingDef("normalAttr", true, true),
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:Normal", "/uudbaaaaa", "val1"));

		UnicoreIncarnationAttributes result = testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().size() == 0);
		assertTrue(result.getValidAttributes().size() == 0);
	}

	@Test
	public void testRegular()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
				new UnicoreAttributeMappingDef("normalAttr", true, true),
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:Normal", "/uudb", "val1"));
		
		UnicoreIncarnationAttributes result = testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().size() == 1 && 
				result.getDefaultAttributes().get("normalAttr") != null && 
				result.getDefaultAttributes().get("normalAttr")[0].equals("val1"));
		assertTrue(result.getValidAttributes().size() == 1 && 
				result.getValidAttributes().get("normalAttr") != null && 
				result.getValidAttributes().get("normalAttr")[0].equals("val1"));
	}

	@Test
	public void testMultiplicity()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
				new UnicoreAttributeMappingDef("normalAttr", false, true),
				new UnicoreAttributeMappingDef("defAttr", false, true),
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		
		attrs.add(createScopedAttr("saml:Normal", "/uudb", "val1", "val2"));
		attrs.add(createScopedAttr("saml:b1", "/uudb", "val1", "val2"));
		attrs.add(createScopedAttr("saml:b1Def", "/uudb", "val1", "val2"));
		
		UnicoreIncarnationAttributes result = testGeneric(mappings, attrs, true, null);
		assertTrue(result.getDefaultAttributes().size() == 2 &&
				result.getValidAttributes().size() == 2	);
		
		assertTrue(result.getDefaultAttributes().get("normalAttr") != null && 
				result.getDefaultAttributes().get("normalAttr")[0].equals("val1") &&
				result.getDefaultAttributes().get("normalAttr").length == 1);
		assertTrue(result.getValidAttributes().get("normalAttr") != null && 
				result.getValidAttributes().get("normalAttr").length == 2);

		assertTrue(result.getDefaultAttributes().get("defAttr") != null && 
				result.getDefaultAttributes().get("defAttr").length == 1 &&
				result.getDefaultAttributes().get("defAttr")[0].equals("val1"));
		assertTrue(result.getValidAttributes().get("defAttr") != null && 
				result.getValidAttributes().get("defAttr").length == 2);
	}

	@Test
	public void testSelectedVo()
	{
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {			
				new UnicoreAttributeMappingDef("normalAttr", true, true),
				new UnicoreAttributeMappingDef("normalAttr2", true, true),
		};
		List<ParsedAttribute> attrs = new ArrayList<ParsedAttribute>();
		attrs.add(createScopedAttr("saml:Normal", "/uudb/aa", "val1"));
		attrs.add(createScopedAttr("saml:Normal", "/uudb", "val1"));
		attrs.add(createScopedAttr("saml:Normal2", "/uudb", "val1"));
		
		UnicoreIncarnationAttributes result = testGeneric(mappings, attrs, true, "/uudb/aa");
		assertEquals(2, result.getDefaultAttributes().size());
		assertEquals(2, result.getValidAttributes().size());
		assertEquals(1, result.getDefaultVoAttributes().size());
		assertEquals("val1", result.getDefaultVoAttributes().get("normalAttr")[0]);
	}

	public static ParsedAttribute createScopedAttr(String name, String scope, String... vals)
	{
		ParsedAttribute ret = new ParsedAttribute(name);
		List<ScopedStringValue> scopedVals = new ArrayList<ScopedStringValue>(vals.length);
		List<String> strVals = new ArrayList<String>(vals.length);
		for (String v: vals)
		{
			scopedVals.add(new ScopedStringValue(scope, SAMLConstants.XACMLDT_STRING, v));
			strVals.add(v);
		}
		ret.setDataType(ScopedStringValue.class);
		ret.setValues(strVals, scopedVals);
		return ret;
	}
	
	private UnicoreIncarnationAttributes testGeneric(UnicoreAttributeMappingDef[] mappings, 
			List<ParsedAttribute> attrs, boolean pullMode, String selectedVo)
	{
		PropertiesBasedConfiguration cfg;
		try
		{
			cfg = new PropertiesBasedConfiguration(
					"src/test/resources/mappingsTest.properties");
			
			UnicoreAttributeMappingDef[] filledMappings = VOCommonUtils.fillMappings(cfg.getSourceProperties(),
					mappings, Log.getLogger("unicore",this.getClass()));
			UnicoreAttributesHandler h = new UnicoreAttributesHandler(cfg, filledMappings, !pullMode);
			return h.extractUnicoreAttributes(attrs, selectedVo, true);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
			return null;
		}
	}
}