package eu.unicore.services.rest.security;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestUserPublicKeyCache {

	@Test
	public void testEDKey() throws Exception {
		String key = FileUtils.readFileToString(new File("src/test/resources/id_ed25519.pub"), "UTF-8");
		assertTrue(UserPublicKeyCache.isValidKey(key));
		key = "from=\"127.0.0.1\" " + key;
		assertTrue(UserPublicKeyCache.isValidKey(key));
	}
}
