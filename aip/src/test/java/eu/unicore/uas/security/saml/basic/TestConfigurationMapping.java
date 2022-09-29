/*
 * Copyright (c) 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on May 16, 2008
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package eu.unicore.uas.security.saml.basic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import eu.unicore.uas.security.saml.UnicoreAttributeMappingDef;
import eu.unicore.uas.security.saml.Utils;
import eu.unicore.uas.security.saml.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;

public class TestConfigurationMapping
{
	@Test
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
			UnicoreAttributeMappingDef[] filledMappings = Utils.fillMappings(
					cfg.getSourceProperties(), mappings, Log.getLogger("unicore",this.getClass()));
			assertTrue(filledMappings.length == 7);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
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
			UnicoreAttributeMappingDef initializedMappings[] = Utils.fillMappings(
					cfg.getSourceProperties(), mappings, Log.getLogger("unicore",this.getClass()));

			for (UnicoreAttributeMappingDef map: initializedMappings)
			{
				if (map.getUnicoreName().equals("disAttr"))
				{
					assertTrue(map.isDisabledInPull());
				} else if (map.getUnicoreName().equals("disAttr2"))
				{
					assertTrue(map.isDisabledInPull());
				} else if (map.getUnicoreName().equals("disAttr3"))
				{
					assertFalse(map.isDisabledInPull());
				} 
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
	}
}