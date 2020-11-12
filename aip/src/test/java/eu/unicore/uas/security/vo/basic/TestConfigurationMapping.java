/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 16, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.vo.basic;

import eu.unicore.uas.security.vo.UnicoreAttributeMappingDef;
import eu.unicore.uas.security.vo.VOCommonUtils;
import eu.unicore.uas.security.vo.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;
import junit.framework.TestCase;


public class TestConfigurationMapping extends TestCase
{
	public void testParse()
	{
		PropertiesBasedConfiguration cfg;
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {
				new UnicoreAttributeMappingDef("disAttr", true, true),
				new UnicoreAttributeMappingDef("disAttr2", true, true),
				new UnicoreAttributeMappingDef("disAttr3", true, true),
				new UnicoreAttributeMappingDef("defAttr", true, true),
				new UnicoreAttributeMappingDef("defAttr2", true, true),
				new UnicoreAttributeMappingDef("defAttr3", true, true),
				new UnicoreAttributeMappingDef("normalAttr", true, true)
			};
		try
		{
			cfg = new PropertiesBasedConfiguration(
					"src/test/resources/mappingsTest.properties");
			UnicoreAttributeMappingDef[] filledMappings = VOCommonUtils.fillMappings(
					cfg.getSourceProperties(), mappings, Log.getLogger("unicore",this.getClass()));
			assertTrue(filledMappings.length == 7);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	
	public void testDisable()
	{
		PropertiesBasedConfiguration cfg;
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {
				new UnicoreAttributeMappingDef("disAttr", true, true),
				new UnicoreAttributeMappingDef("disAttr2", true, true),
				new UnicoreAttributeMappingDef("disAttr3", true, true)
			};
		try
		{
			cfg = new PropertiesBasedConfiguration(
					"src/test/resources/mappingsTest.properties");
			UnicoreAttributeMappingDef initializedMappings[] = VOCommonUtils.fillMappings(
					cfg.getSourceProperties(), mappings, Log.getLogger("unicore",this.getClass()));

			for (UnicoreAttributeMappingDef map: initializedMappings)
			{
				if (map.getUnicoreName().equals("disAttr"))
				{
					assertTrue(map.isDisabledInPull());
					assertTrue(map.isDisabledInPush());					
				} else if (map.getUnicoreName().equals("disAttr2"))
				{
					assertTrue(map.isDisabledInPull());
					assertFalse(map.isDisabledInPush());
				} else if (map.getUnicoreName().equals("disAttr3"))
				{
					assertFalse(map.isDisabledInPull());
					assertTrue(map.isDisabledInPush());
				} 
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
}