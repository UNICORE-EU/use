package eu.unicore.services.rest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class TestUtils {

	@Test
	public void testUserPreferences(){
		UserPreferences p = new UserPreferences();
		assertEquals("", p.getEncoded());
		p.setUid("test");
		assertEquals("uid:test", p.getEncoded());
		p.setRole("admin");
		assertTrue(p.getEncoded().contains(("uid:test")));
		assertTrue(p.getEncoded().contains(("role:admin")));
		assertTrue(p.getEncoded().contains((",")));
	}
	
	@Test
	public void testRESTException() {
		RESTException re = new RESTException(500, "Server error", "some more details");
		assertEquals("some more details [HTTP 500 Server error]", re.getMessage());
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
		assertTrue(err.contains("Failed to process POST request"));
	}

	@Test
	public void testJSONConversions() throws Exception {
		JSONObject o = new JSONObject("{foo: 123, bar: \"456\"}");
		Map<String,String> map = BaseClient.asMap(o);
		assertEquals("123", map.get("foo"));
		assertEquals("456", map.get("bar"));
	}
}
