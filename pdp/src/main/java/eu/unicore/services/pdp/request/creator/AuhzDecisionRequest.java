package eu.unicore.services.pdp.request.creator;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;

import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.proto.AbstractRequest;
import eu.unicore.security.dsig.DSigException;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLAuthzDecisionQueryType;

/**
 * SAML XACML Authz Decision Query representation.
 * @author golbi
 */
public class AuhzDecisionRequest extends AbstractRequest<XACMLAuthzDecisionQueryDocument, XACMLAuthzDecisionQueryType>
{
	private XACMLAuthzDecisionQueryDocument xbdoc;

	public AuhzDecisionRequest(NameID issuer)
	{
		xbdoc = XACMLAuthzDecisionQueryDocument.Factory.newInstance();
		XACMLAuthzDecisionQueryType xad=xbdoc.addNewXACMLAuthzDecisionQuery();
		init(xbdoc, xad, issuer.getXBean());
	}

	@Override
	public void sign(PrivateKey pk, X509Certificate[] cert) 
		throws DSigException
	{
		Document doc = signInt(pk, cert);
		try
		{
			xbdoc = XACMLAuthzDecisionQueryDocument.Factory.parse(doc);
			xmlReq = xbdoc.getXACMLAuthzDecisionQuery();
		} catch (XmlException e)
		{
			throw new DSigException("Parsing signed document failed", e);
		}
	}
	
}