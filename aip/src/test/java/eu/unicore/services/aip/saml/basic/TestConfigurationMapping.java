package eu.unicore.services.aip.saml.basic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.unicore.services.aip.saml.UnicoreAttributeMappingDef;
import eu.unicore.services.aip.saml.Utils;
import eu.unicore.services.aip.saml.conf.PropertiesBasedConfiguration;
import eu.unicore.util.Log;

public class TestConfigurationMapping
{
	@Test
	public void testParse() throws Exception {
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
		cfg = new PropertiesBasedConfiguration(
				"src/test/resources/mappingsTest.properties");
		UnicoreAttributeMappingDef[] filledMappings = Utils.fillMappings(
				cfg.getSourceProperties(), mappings, Log.getLogger("unicore",this.getClass()));
		assertTrue(filledMappings.length == 7);
	}

	@Test
	public void testDisable() throws Exception {
		PropertiesBasedConfiguration cfg;
		UnicoreAttributeMappingDef[] mappings = new UnicoreAttributeMappingDef[] {
				new UnicoreAttributeMappingDef("disAttr", true, true),
				new UnicoreAttributeMappingDef("disAttr2", true, true),
				new UnicoreAttributeMappingDef("disAttr3", true, true)
		};
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
	}
}