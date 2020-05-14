package eu.unicore.uas.security.gridmapfile;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import de.fzj.unicore.wsrflite.Kernel;
import static de.fzj.unicore.wsrflite.security.ContainerSecurityProperties.*;
import de.fzj.unicore.wsrflite.security.IAttributeSource;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.security.util.AttributeSourcesChain;
import eu.unicore.util.configuration.ConfigurationException;

public class TestGridmapFileAuthoriser {

	@Test(expected=ConfigurationException.class)
	public void testErrorMissingFile()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PREFIX+PROP_AIP_ORDER, "GMF");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.class", GridMapFileAuthoriser.class.getName());
		new Kernel(p);
	}
	
	@Test
	public void testCorrectSetup()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(PREFIX+PROP_AIP_ORDER, "GMF");
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.class", GridMapFileAuthoriser.class.getName());
		p.setProperty(PREFIX+PROP_AIP_PREFIX+".GMF.file", "src/test/resources/gridmapfile/grid-mapfile");
		Kernel k=new Kernel(p);
		IAttributeSource attrSource=k.getContainerSecurityConfiguration().getAip();
		assertNotNull(attrSource);
		AttributeSourcesChain chain=(AttributeSourcesChain)attrSource;
		GridMapFileAuthoriser gmf=(GridMapFileAuthoriser)chain.getChain().get(0);
		assertNotNull(gmf);
	}
}
