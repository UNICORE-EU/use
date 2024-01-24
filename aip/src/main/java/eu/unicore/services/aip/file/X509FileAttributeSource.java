package eu.unicore.services.aip.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.security.IAttributeSource;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;


/**
 * This is a variation of {@link FileAttributeSource} using full X509 certificates for
 * comparison instead of just the DN.
 * 
 * The Client's attributes are retrieved from a file. The file format is quite simple:
 * 
 * <pre>
 *  &lt;fileAttributeSource&gt;
 *   &lt;entry&gt;
 *     &lt;key&gt;pem encoded x509 certificate&lt;/key&gt;
 *     &lt;attribute name="xlogin"&gt;
 *       &lt;value&gt;nobody&lt;/value&gt;
 *       &lt;value&gt;somebody&lt;/value&gt;
 *     &lt;/attribute&gt;
 *     &lt;attribute name="role"&gt;&lt;value&gt;user&lt;/value&gt;&lt;/attribute&gt;
 *   &lt;/entry&gt;
 * &lt;/fileAttributeSource&gt;
 * </pre>
 * 
 * Note that the "key" is written as an XML element. 
 * You can add arbitrary number of attributes and attribute values.
 * <p>
 * Configuration of this source consist of a single entry:
 * <ul>
 * <li>file - the path of the described above file with attributes</li>
 * </ul>
 * <p>
 * Evaluation compares the effective user X509 with the X509 used as key, using the equals() 
 * method of X509Certificate (rather its BouncyCastle implementation)
 * <p>
 * The attributes file is automatically refreshed after any change, before subsequent read. If the
 * syntax is wrong then loud message is logged and old version is used.
 * <p>
 * Some attribute names are special: xlogin, role, group, supplementaryGroups, addOsGroups, queue.
 * Attributes with those names (case insensitive)
 * are handled as those special UNICORE attributes (e.g. xlogin is used to provide available local OS 
 * user names for the client).
 * <p>
 * All other attributes are treated as XACML authorization attributes of String type and are
 * passed to the PDP. Such attributes must have at least one value to be processed.
 * 
 * @author golbi
 * @author schuller
 */
public class X509FileAttributeSource extends FileAttributeSourceBase implements IAttributeSource 
{
	private static final Logger logger = Log.getLogger(Log.SECURITY, X509FileAttributeSource.class);

	private Map<X509Certificate, List<Attribute>> map;

	public X509FileAttributeSource() {
		super(logger);
	}

	private Map<X509Certificate, List<Attribute>> convert(Map<String, List<Attribute>>rawmap) 
			throws IOException {
		Map<X509Certificate, List<Attribute>>result = new HashMap<>();
		for(Map.Entry<String, List<Attribute>> e: rawmap.entrySet()){
			String key=e.getKey();
			InputStream is = new ByteArrayInputStream(key.getBytes());
			try {
				X509Certificate c = CertificateUtils.loadCertificate(is, Encoding.PEM);
				if (logger.isDebugEnabled())
					logger.debug("Loaded " + X500NameUtils.getReadableForm(
							c.getSubjectX500Principal()));
				result.put(c, e.getValue());
			} catch (IOException ex) {
				throw new ConfigurationException("This key is invalid, should be a PEM " +
						"certificate: " + key, ex);
			}
		}
		return result;
	}
	
	@Override
	public SubjectAttributesHolder getAttributes(SecurityTokens tokens,
			SubjectAttributesHolder otherAuthoriserInfo) throws IOException
	{
		parseIfNeeded();
		
		X509Certificate subject = tokens.getEffectiveUserCertificate();
		if (tokens.getUserCertificate() == null)
		{
			logger.info("No effective user certificate found, won't assign any attributes " +
					"from the certificate file attribute source. The client effective DN is: " + 
					tokens.getEffectiveUserName());
			subject = null;
		}
		List<Attribute> attrs = searchFor(subject);
		
		Map<String, String[]> retAll = new HashMap<String, String[]>();
		Map<String, String[]> retFirst = new HashMap<String, String[]>();
		List<XACMLAttribute> retXACML = new ArrayList<XACMLAttribute>();
		if (attrs != null)
			putAttributes(attrs, retAll, retFirst, retXACML);
		return new SubjectAttributesHolder(retXACML, retFirst, retAll);
	}

	private List<Attribute> searchFor(X509Certificate cert)
	{
		return map.get(cert);
	}

	@Override
	protected void installNewMappings(Map<String, List<Attribute>> newData) throws ConfigurationException
	{
		try
		{
			map = convert(newData);
		} catch (Exception ex) 
		{
			throw new ConfigurationException("Can't parse key(s) from the uudb file as certificates: " 
					+ ex, ex);
		}
	}

}








