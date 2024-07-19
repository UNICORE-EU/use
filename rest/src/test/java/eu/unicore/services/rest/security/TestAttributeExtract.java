package eu.unicore.services.rest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.services.rest.RESTUtils;
public class TestAttributeExtract {

	@Test
	public void test1() {
		String t1 = "{'sub':'abc123','username_attr':'s.user@some.org',"
				+ "'account_type': 'normal',"
				+ "'details':['user1,system1,project1,s.user',"
				+ "'user1,system2,project1,s.user',"
				+ "'user1,system2,project2,s.user'],"
				+ "'x500name':'UID=s.user@some.org'}";
		String script1 = "dets = attrs['details']; for(d: dets){"
				+ "tok = d.split(',');"
				+ "if('system1'.equals(tok[1]))return tok[0];"
				+ "};"
				+ "return null";
		
		JSONObject o = new JSONObject(t1);
		var attrs = RESTUtils.asMap2(o);
		var context = new HashMap<String,Object>();
		context.put("attrs", attrs);
		assertEquals("normal", attrs.get("account_type"));
		String uid = RESTUtils.evaluateToString(script1, context);
		assertEquals("user1", uid);
		
		String script2 = "dets = attrs['details'];"
				+ "res = new java.util.ArrayList();"
				+ "for(d: dets){"
				+ "  tok = d.split(',');"
				+ "  if('system2'.equals(tok[1]))res.add(tok[2]);"
				+ "};"
				+ "return res;";
		String[] groups = RESTUtils.evaluateToArray(script2, context);
		assertArrayEquals(new String[] {"project1", "project2"}, groups);
	}
}
