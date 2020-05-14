package eu.unicore.uas.pdp.argus;

import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryDocument;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;

public class PolicyRequestCreator {


	public static XACMLPolicyQueryDocument createSAMLPolicyQuery(String queryIssuerName)
	{
		PolicyRequest req = new PolicyRequest(new NameID(queryIssuerName, 
				SAMLConstants.NFORMAT_ENTITY));
		return req.getXMLBeanDoc();
	}

}
