package eu.unicore.services.rest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

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
		UserInfoSource uis = (username, dnAssign) -> {
			AttributesHolder ah = new AttributesHolder(username);
			if(haveKey.get()) {
				ah.getPublicKeys().add(key);
			}
			return ah;
		};
		UserPublicKeyCache  upkc = new UserPublicKeyCache();
		upkc.getUserInfoSources().add(uis);
		AttributeHolders ahs = upkc.get("testuser");
		AttributesHolder ah = ahs.get().get(0);
		assertEquals("CN=testuser, OU=ssh-local-users", ah.getDN());
		assertFalse(ah.fromFile);
		assertEquals("testuser", ah.user);
		assertEquals(key, ah.getPublicKeys().get(0));
		ahs.invalidate();
		haveKey.set(false);
		ahs = upkc.get("testuser");
		assertEquals(0, ahs.get().size());
		ahs.invalidate();
		haveKey.set(true);
		ahs = upkc.get("testuser");
		assertEquals(1, ahs.get().size());
	}
	
	@Test
	public void testIdentityMapping() throws Exception {
		String key = FileUtils.readFileToString(new File("src/test/resources/id_ed25519.pub"), "UTF-8");
		Map<String, Object>params = new HashMap<>();
		params.put("email", "foo@bar.org");
		String dnAssign = "'UID='+email";
		UserInfoSource uis = (username, x) -> {
			AttributesHolder ah = AttributesHolder.fromAttributes(username, dnAssign, params);
			ah.getPublicKeys().add(key);
			return ah;
		};
		UserPublicKeyCache  upkc = new UserPublicKeyCache();
		upkc.setIdentityAssign(dnAssign);
		upkc.getUserInfoSources().add(uis);
		AttributeHolders ahs = upkc.get("testuser");
		AttributesHolder ah = ahs.get().get(0);
		assertFalse(ah.fromFile);
		assertEquals("testuser", ah.user);
		assertEquals(key, ah.getPublicKeys().get(0));
		assertEquals("UID=foo@bar.org", ah.getDN());
	}
}