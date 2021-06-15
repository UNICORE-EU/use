package eu.unicore.uas.pdp.argus;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;

import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.proto.AbstractRequest;
import eu.unicore.security.dsig.DSigException;
import xmlbeans.oasis.xacml.x2.x0.policy.IdReferenceType;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryDocument;
import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryType;

public class PolicyRequest extends AbstractRequest<XACMLPolicyQueryDocument, XACMLPolicyQueryType> {
	
	private XACMLPolicyQueryDocument xbdoc;
	
	public PolicyRequest(NameID issuer)
	{
		xbdoc = XACMLPolicyQueryDocument.Factory.newInstance();
		xbdoc.addNewXACMLPolicyQuery();
		XACMLPolicyQueryType xreq=xbdoc.getXACMLPolicyQuery();
		xreq.setIssueInstant(Calendar.getInstance());
		IdReferenceType ref=xreq.addNewPolicyIdReference();
		ref.setStringValue("-1");
		init(xbdoc, xreq, issuer.getXBean());
	}

	@Override
	public void sign(PrivateKey key, X509Certificate[] cert)
			throws DSigException {
		Document doc = signInt(key, cert);
		try
		{
			xbdoc = XACMLPolicyQueryDocument.Factory.parse(doc);
			xmlReq = xbdoc.getXACMLPolicyQuery();
		} catch (XmlException e)
		{
			throw new DSigException("Parsing signed document failed", e);
		}
		
	}

}
