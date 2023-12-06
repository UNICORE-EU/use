package eu.unicore.uas.security.xuudb;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzJuelich.unicore.xuudb.CheckCertificateDocument;
import de.fzJuelich.unicore.xuudb.CheckCertificateResponseDocument;
import de.fzJuelich.unicore.xuudb.CheckDNDataType;
import de.fzJuelich.unicore.xuudb.CheckDNDocument;
import de.fzJuelich.unicore.xuudb.CheckDNResponseDocument;
import de.fzJuelich.unicore.xuudb.CheckDataType;
import de.fzJuelich.unicore.xuudb.LoginDataType;
import de.fzj.unicore.xuudb.X509Utils;
import de.fzj.unicore.xuudb.interfaces.IPublic;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.SubsystemUnavailableException;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * get user attributes from an XUUDB
 *
 * @author schuller
 * @author golbi
 */
public class XUUDBAttributeSource extends XUUDBBase implements
		IAttributeSource {

	private IPublic xuudb;

	@Override
	public void start(Kernel kernel) throws Exception {
		this.kernel = kernel;
		xuudb = makeEndpoint();
		isEnabled = xuudb!=null;
	}

	public void setEndpoint(IPublic xuudb) {
		this.xuudb = xuudb;
	}

	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException {
		if (!isEnabled)
			throw new SubsystemUnavailableException("Attribute source "+name+" is disabled");
		
		if(!cb.isOK())
			throw new SubsystemUnavailableException("Attribute source "+name+" unavailable");
		
		SubjectAttributesHolder map;
		X509Certificate cert = (X509Certificate) tokens.getEffectiveUserCertificate();
		boolean workUsingCertificate = tokens.getUserCertificate() != null;
		if (workUsingCertificate) {
			map = cacheCredentials ? cache.read(cert) : null;
			if (map == null) {
				map = checkUserCert(tokens);
				if (cacheCredentials) {
					cache.put(cert, map);
				}
			}
		} else {
			map = cacheCredentials ? cache.read(tokens.getEffectiveUserName())
					: null;
			if (map == null) {
				map = checkDN(tokens);
				if (cacheCredentials) {
					cache.put(tokens.getEffectiveUserName(), map);
				}
			}
		}

		return map;
	}

	/**
	 * retrieves user attributes using the user certificate
	 * 
	 * @param tokens
	 * @return SubjectAttributesHolder
	 */
	protected SubjectAttributesHolder checkUserCert(final SecurityTokens tokens)
			throws IOException {

		CheckCertificateDocument in = CheckCertificateDocument.Factory
				.newInstance();
		CheckDataType check = in.addNewCheckCertificate();
		check.setGcID(gcID);
		X509Certificate cert = tokens.getEffectiveUserCertificate();
		if (cert == null) {
			return checkDN(tokens);
		}
		addAccessorName(cert.getSubjectX500Principal().getName());
		CheckCertificateResponseDocument res = null;
		try {
			check.setCertInPEM(X509Utils.getPEMStringFromX509(cert));
			if (logger.isDebugEnabled()) {
				logger.debug("XUUDB request: " + in.toString());
			}
			synchronized (xuudb) {
				res = xuudb.checkCertificate(in);
			}
		} catch (Exception e) {
			cb.notOK();
			throw new IOException("Error contacting "+ name, e);
		}
		if (logger.isDebugEnabled()) {
			LoginDataType login = res.getCheckCertificateResponse();
			String reply = "[xlogin=" + login.getXlogin() + ", role="
					+ login.getRole() + ", projects=" + login.getProjects()
					+ "]";
			logger.debug("XUUDB reply: " + reply);
		}
		SubjectAttributesHolder map = makeAuthInfo(res
				.getCheckCertificateResponse());
		return map;
	}

	/**
	 * retrieves user attributes using the users's DN
	 * 
	 * @param tokens
	 * @return SubjectAttributesHolder
	 */
	protected SubjectAttributesHolder checkDN(final SecurityTokens tokens)
			throws IOException {
		CheckDNDocument in = CheckDNDocument.Factory.newInstance();
		CheckDNDataType check = in.addNewCheckDN();
		check.setGcID(gcID);
		String dn = tokens.getEffectiveUserName();
		addAccessorName(dn);
		check.setDistinguishedName(dn);
		CheckDNResponseDocument res = null;
		if (logger.isDebugEnabled()) {
			logger.debug(name + " request: " + in.toString());
		}
		synchronized (xuudb) {
			try {
				res = xuudb.checkDN(in);
			} catch (Exception e) {
				cb.notOK();
				throw new IOException("Error contacting "+name, e);
			}
		}
		if (logger.isDebugEnabled()) {
			LoginDataType login = res.getCheckDNResponse();
			String reply = "[xlogin=" + login.getXlogin() + ", role="
					+ login.getRole() + ", projects=" + login.getProjects()
					+ "]";
			logger.debug("{} reply: {}", name, reply);
		}
		return makeAuthInfo(res.getCheckDNResponse());
	}

	/**
	 * parse reply from uudb and return a map of auth info
	 */
	public SubjectAttributesHolder makeAuthInfo(LoginDataType login) {
		String role = login.getRole();
		String xlogin = login.getXlogin();
		String groups = login.getProjects();
		Map<String, String[]> map = new HashMap<>();
		Map<String, String[]> mapDef = new HashMap<>();
		if (isNotEmpty(xlogin)) {
			String[] xlogins = decode(xlogin);
			map.put(IAttributeSource.ATTRIBUTE_XLOGIN, xlogins);
			if (xlogins.length > 0)
				mapDef.put(IAttributeSource.ATTRIBUTE_XLOGIN,
						new String[] { xlogins[0] });
		}
		if (isNotEmpty(role)) {
			String[] roles = decode(role);
			map.put(IAttributeSource.ATTRIBUTE_ROLE, roles);
			mapDef.put(IAttributeSource.ATTRIBUTE_ROLE,
					new String[] { roles[0] });
		}
		if (isNotEmpty(groups)) {
			String[] gids = decode(groups);
			map.put(IAttributeSource.ATTRIBUTE_GROUP, gids);
			mapDef.put(IAttributeSource.ATTRIBUTE_GROUP,
					new String[] { gids[0] });
		}

		return new SubjectAttributesHolder(mapDef, map);
	}

	protected String[] decode(String in) {
		List<String> res = new ArrayList<>();
		for (String s : in.split(":")) {
			String v = s.trim();
			if (v.length() != 0)
				res.add(v);
		}
		return res.toArray(new String[res.size()]);
	}

	protected synchronized IPublic makeEndpoint() {
		if (xuudb != null)
			return xuudb;
		try {
			IClientConfiguration sec = kernel.getClientConfiguration();
			return new WSClientFactory(sec).createPlainWSProxy(
					IPublic.class, getXUUDBUrl() + IPublic.SERVICE_NAME);
		} catch (Exception e) {
			Log.logException("Can't make connection to " + name,
					e, logger);
			return null;
		}
	}

}
