package eu.unicore.services.rest.client;

import org.junit.Test;

import junit.framework.Assert;

public class TestUtils {

	@Test
	public void testUserPreferences(){
		UserPreferences p = new UserPreferences();
		Assert.assertEquals("", p.getEncoded());
		p.setUid("test");
		Assert.assertEquals("uid:test", p.getEncoded());
		p.setRole("admin");
		Assert.assertTrue(p.getEncoded().contains(("uid:test")));
		Assert.assertTrue(p.getEncoded().contains(("role:admin")));
		Assert.assertTrue(p.getEncoded().contains((",")));
	}
	
	@Test
	public void testRESTException() {
		RESTException re = new RESTException(500, "Server error", "some more details");
		Assert.assertEquals("some more details [HTTP 500 Server error]", re.getMessage());
	}
}
