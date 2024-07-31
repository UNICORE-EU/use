package eu.unicore.services.aip.gridmapfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;

import eu.emi.security.authn.x509.impl.OpensslNameUtils;
import eu.unicore.util.Pair;


public class TestGridMapFileParsing {


	@Test
	public void testSingleLine() throws Exception{
		Pair<String,String[]> mapping = GridMapFileParser.parseLine("\"/C=DE/O=GridGermany/OU=Forschungszentrum Juelich GmbH/CN=Morris Riedel\" communig");
		X500Principal x500 = new X500Principal("CN=Morris Riedel,OU=Forschungszentrum Juelich GmbH,O=GridGermany,C=DE");
		String normalizedDN = OpensslNameUtils.normalize(OpensslNameUtils.convertFromRfc2253(x500.getName(), true));
		assertEquals(normalizedDN, mapping.getM1());
		assertEquals("communig", mapping.getM2()[0]);
	}

	@Test
	public void testWholeFile() throws Exception{
		GridMapFileParser parser = new GridMapFileParser(new File("src/test/resources/gridmapfile/grid-mapfile"));
		Map<String,List<String>> map = parser.parse();
		assertEquals(0, parser.getNumError());
		
		X500Principal x500 = new X500Principal("CN=Mace Windu,OU=Jedi Council,O=Jedi Order,O=Grid");
		String normalizedDN = OpensslNameUtils.normalize(OpensslNameUtils.convertFromRfc2253(x500.getName(), true));
		List<String> logins = map.get(normalizedDN);
		assertNotNull(logins);
		assertEquals(1, logins.size());
		assertEquals("weddie", logins.get(0));

		x500 = new X500Principal("CN=Darth Sidious,OU=Sith Master,O=Sith Order,O=Grid");
		normalizedDN = OpensslNameUtils.normalize(OpensslNameUtils.convertFromRfc2253(x500.getName(), true));

		logins = map.get(normalizedDN);
		assertNotNull(logins);
		assertEquals(2, logins.size());
		assertEquals("weddie", logins.get(0));
		assertEquals("admin", logins.get(1));
	}	

}





