package eu.unicore.services.aip.gridmapfile;

import static eu.unicore.services.security.ContainerSecurityProperties.PREFIX;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_ORDER;
import static eu.unicore.services.security.ContainerSecurityProperties.PROP_AIP_PREFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.security.util.AttributeSourcesChain;
import eu.unicore.util.configuration.ConfigurationException;

public class TestGridmapFileAuthoriser {

	@Test
	public void testErrorMissingFile()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PREFIX+PROP_AIP_ORDER, "GMF");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.class", GridMapFileAttributeSource.class.getName());
		assertThrows(ConfigurationException.class,()->{
			new Kernel(p);
		});
	}
	
	@Test
	public void testCorrectSetup()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PREFIX+PROP_AIP_ORDER, "GMF");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.class", GridMapFileAttributeSource.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.file", "src/test/resources/gridmapfile/grid-mapfile");
		Kernel k=new Kernel(p);
		IAttributeSource attrSource=k.getSecurityManager().getAip();
		assertNotNull(attrSource);
		AttributeSourcesChain chain=(AttributeSourcesChain)attrSource;
		GridMapFileAttributeSource gmf=(GridMapFileAttributeSource)chain.getChain().get(1);
		assertNotNull(gmf);
	}
}
