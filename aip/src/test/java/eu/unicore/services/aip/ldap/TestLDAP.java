package eu.unicore.services.aip.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.InitialContextFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.Test;

import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;

public class TestLDAP {

	static Mockery ctx;
	
	private LDAPAttributeSource init() throws Exception
	{
		LDAPAttributeSource ldap = new LDAPAttributeSource();
		ldap.setCtxFactory(MockCtxFactory.class.getName());
		ldap.setLdapURL("ldap://mock");
		ldap.setLdapFilter("");
		ldap.setLdapDNAttrName("x500Name");
		ldap.setLdapLoginAttrName("username");
		ldap.setLdapRoleAttrName("role");
		ldap.setLdapGroupAttrName("projects");
		ldap.setLdapGroupDefaultValue("");
		ldap.setLdapRoleDefaultValue("");
		Kernel k = new Kernel(TestConfigUtil.getInsecureProperties());
		ldap.configure("LDAP", k);
		return ldap;
	}

	@Test
	public void testBasicLDAP() throws Exception {
		ctx = new JUnit5Mockery();
		LDAPAttributeSource ldap = init();
		assertNotNull(ldap);
		System.out.println(ldap.toString());
		DirContext dirCtx = ldap.makeEndpoint();
		assertNotNull(dirCtx);
		Attributes attr = new BasicAttributes();
		attr.put("role", "user");
		attr.put("username", "demo");
		attr.put("projects", "demo");
		ctx.checking(new Expectations() {{
			allowing(MockCtxFactory.dc).search(
					with(any(String.class)),
					with(any(String.class)),
					with(any(SearchControls.class)));
			will(returnValue(new Result(new SearchResult("CN=Test", new Object(), attr))));
		}});
		SecurityTokens tokens = new SecurityTokens();
		tokens.setUserName("CN=Test");
		tokens.setConsignorTrusted(true);
		SubjectAttributesHolder holder = ldap.getAttributes(tokens, null);
		ctx.assertIsSatisfied();
		Map<String,String[]>userAttr = holder.getIncarnationAttributes();
		assertEquals("demo", userAttr.get("xlogin")[0]);
		assertEquals("user", userAttr.get("role")[0]);
		assertEquals("demo", userAttr.get("group")[0]);
		System.out.println(ldap.getExternalSystemName()+" "+ldap.getConnectionStatusMessage()+" "+ldap.getConnectionStatus());
	}

	public static class MockCtxFactory implements InitialContextFactory {
		public static DirContext dc = null;
		public synchronized Context getInitialContext(Hashtable<?,?> environment) throws NamingException {
			if(dc==null)dc=ctx.mock(DirContext.class);
			return dc;
		}
	}

	public static class Result implements NamingEnumeration<SearchResult>{
		SearchResult res;
		public Result(SearchResult res) {
			this.res=res;
		}

		@Override
		public boolean hasMoreElements() {
			return res!=null;
		}

		@Override
		public SearchResult nextElement() {
			SearchResult r = res;
			res=null;
			return r;
		}

		@Override
		public SearchResult next() throws NamingException {
			SearchResult r = res;
			res=null;
			return r;
		}

		@Override
		public boolean hasMore() throws NamingException {
			return res!=null;
		}

		@Override
		public void close() throws NamingException {}
	}

}
