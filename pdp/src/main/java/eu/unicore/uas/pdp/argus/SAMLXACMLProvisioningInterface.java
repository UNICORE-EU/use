package eu.unicore.uas.pdp.argus;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;


@WebService(name="XACMLPolicy", 
		targetNamespace="", 
		serviceName="")
	@SOAPBinding(parameterStyle=SOAPBinding.ParameterStyle.BARE)
public interface SAMLXACMLProvisioningInterface {
	@WebMethod(action="http://www.oasis-open.org/committees/security")
	public ResponseDocument policyRequest(XACMLPolicyQueryDocument reqDoc);

}
