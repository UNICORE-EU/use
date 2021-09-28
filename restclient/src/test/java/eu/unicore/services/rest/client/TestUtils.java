package eu.unicore.services.rest.client;

import org.junit.Assert;
import org.junit.Test;

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
	
	@Test
	public void testRESTExceptionHTML() {
		String html = "<html>\n<head>\n" + 
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n" + 
				"<title> HTTP Error: 500 javax.servlet.ServletException: Failed to process POST request: "
				+ "javax.servlet.ServletException: Problem when forwarding a client request to a VSite: "
				+ "org.apache.http.conn.HttpHostConnectException: "
				+ "Connect to localhost:7778 [localhost/127.0.0.1] failed: "
				+ "Connection refused (Connection refused)</title>\n" + 
				"</head>\n" + 
				"<body><h1 style=\"color: red;\">HTTP Error: 500</h1>Error reason:\n";
		String err = BaseClient.extractHTMLError(html);
		Assert.assertTrue(err.contains("Failed to process POST request"));
	}
}
