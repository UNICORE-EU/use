package eu.unicore.services.restclient.utils;

import org.junit.jupiter.api.Test;

public class TestUserLogger {

	@Test
	public void testUserLogger () {
		UserLogger u = new UserLogger() {};
		u.info("{}", "test");
		u.debug("{}", "test");
		u.verbose("{}", "test");
		u.error(new Exception(), "", "");
	}

}