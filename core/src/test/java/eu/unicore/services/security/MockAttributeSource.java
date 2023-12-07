package eu.unicore.services.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.Kernel;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * for testing. Data is hardcoded into several sets. The set is chosen with the only config
 * parameter 'set'.
 * <br>
 * Additionally the class allows for (dirty) static overriding of role and xlogin.
 */
public class MockAttributeSource implements IAttributeSource{

	private int set = 0;
	private static String[] role;
	private static String[] xlogin;
	
	public MockAttributeSource(){}

	/**
	 * gets attributes based on user's DN
	 * 
	 * @throws AuthorisationException
	 */
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo)
			throws IOException
	{
		String name=tokens.getEffectiveUserName();
		String key=cnDn(name);
		Map<String, String[]> ret;
		switch (set) {
		case 0:
			ret = getAttributesSet0(key);
			break;
		default:
			throw new IOException("Configured set " + set + " is unimplemented");
		}
		if (xlogin != null)
			ret.put(IAttributeSource.ATTRIBUTE_XLOGIN, xlogin);
		if (role != null)
			ret.put(IAttributeSource.ATTRIBUTE_ROLE, role);
		return new SubjectAttributesHolder(ret, ret);
	}
	
	private Map<String, String[]> getAttributesSet0(String key) {
		Map<String, String[]> ret = new HashMap<String, String[]>();
		if (key.equals(cnDn("cn=unicore demo unicorex,o=unicore.eu,ou=testing")))
		{
			ret.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"nobody"});
			ret.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{"user"});
		} else if (key.equals(cnDn("EMAILADDRESS=unicore-support@lists.sf.net, C=DE, O=unicore.eu, OU=Testing, CN=UNICORE demo user")))
		{
			ret.put(IAttributeSource.ATTRIBUTE_XLOGIN, new String[]{"nobody", "somebody"});
			ret.put(IAttributeSource.ATTRIBUTE_ROLE, new String[]{"user", "boozer"});
			ret.put(IAttributeSource.ATTRIBUTE_GROUP, new String[]{"ola", "tola", "staff", "wheel"});
		}
		return ret;
	}
	
	
	
		
	private static String cnDn(String dn) {
		return new X500Principal(dn).getName(X500Principal.CANONICAL);
	}
	
	public String getStatusDescription(){
		return "OK";
	}

	public int getSet() {
		return set;
	}

	public void setSet(int set) {
		this.set = set;
	}

	public String getName() {
		return "Mock Attribute Source";
	}

	public static void setRole(String[] role){
		MockAttributeSource.role=role;
	}

	public static void setXlogin(String[] x){
		MockAttributeSource.xlogin=x;
	}

	@Override
	public void configure(String name, Kernel k) throws ConfigurationException {}

}
