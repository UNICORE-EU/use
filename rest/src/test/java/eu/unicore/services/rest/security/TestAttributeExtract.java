package eu.unicore.services.rest.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.services.rest.RESTUtils;

public class TestAttributeExtract {

	@Test
	public void test1() {
		var t = "{'sub':'abc123','username_attr':'s.user@some.org',"
				+ "'account_type': 'normal',"
				+ "'details':['user1,system1,project1,s.user',"
				+ "'user1,system2,project1,s.user',"
				+ "'user1,system2,project2,s.user'],"
				+ "'x500name':'UID=s.user@some.org'}";
		var script1 = "for(d: details){"
				+ "tok = d.split(',');"
				+ "if('system1'.equals(tok[1]))return tok[0];"
				+ "};"
				+ "return null";

		var o = new JSONObject(t);
		var context = RESTUtils.asMap2(o);
		assertEquals("normal", context.get("account_type"));
		assertEquals("user1", RESTUtils.evaluateToString(script1, context));

		var script2 = "res = [];"
				+ "for(d: details){"
				+ "  tok = d.split(',');"
				+ "  if('system2'==tok[1])res+=tok[2];"
				+ "};"
				+ "res";
		assertArrayEquals(new String[] {"project1", "project2"},
				RESTUtils.evaluateToArray(script2, context));
	}

	@Test
	public void test2() {
		var t = "{'details':[ 'urn:sth:res:SYSTEM:proj:act:xlogin:normal'],"
				+ "'email':'s.user@some.org',"
				+ "'preferred_username':'user1'}";
		var context = RESTUtils.asMap2(new JSONObject(t));
		var dnAssign = "\"UID=\"+email+(details[0].endsWith('normal')?\"\":\",OU=\"+preferred_username)";
		assertEquals("UID=s.user@some.org", RESTUtils.evaluateToString(dnAssign, context));

		t = "{'details':[ 'urn:sth:res:SYSTEM:proj:act:xlogin:special_snowflake'],"
				+ "'email':'s.user@some.org',"
				+ "'preferred_username':'user1'}";
		context = RESTUtils.asMap2(new JSONObject(t));
		System.err.println("Script: "+dnAssign);
		assertEquals("UID=s.user@some.org,OU=user1", RESTUtils.evaluateToString(dnAssign, context));
	}
}
