package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import eu.unicore.services.rest.security.UserPublicKeyCache.AttributeHolders;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributesHolder;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;

public class TestUserPublicKeyCache {

	@Test
	public void testEDKey() throws Exception {
		String key = FileUtils.readFileToString(new File("src/test/resources/id_ed25519.pub"), "UTF-8");
		assertTrue(UserPublicKeyCache.isValidKey(key));
		String key2 = "from=\"127.0.0.1\" " + key;
		assertTrue(UserPublicKeyCache.isValidKey(key2));
		String key3 = "notvalid";
		assertFalse(UserPublicKeyCache.isValidKey(key3));
	}

	@Test
	public void testExternalKeySource() throws Exception {
		String key = FileUtils.readFileToString(new File("src/test/resources/id_ed25519.pub"), "UTF-8");
		final AtomicBoolean haveKey = new AtomicBoolean(true);
		UserInfoSource uis = (username) -> {
			return haveKey.get() ? Lists.newArrayList(key) : new ArrayList<>();
		};
		UserPublicKeyCache  upkc = new UserPublicKeyCache();
		upkc.getUserInfoSources().add(uis);
		AttributeHolders ahs = upkc.get("testuser");
		AttributesHolder ah = ahs.get().get(0);
		assertEquals("CN=testuser, OU=ssh-local-users", ah.dn);
		assertFalse(ah.fromFile);
		assertEquals("testuser", ah.user);
		assertEquals(key, ah.sshkey);
		ahs.invalidate();
		haveKey.set(false);
		ahs = upkc.get("testuser");
		assertEquals(0, ahs.get().size());
		ahs.invalidate();
		haveKey.set(true);
		ahs = upkc.get("testuser");
		assertEquals(1, ahs.get().size());
	}
}