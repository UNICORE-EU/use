package eu.unicore.uas.pdp.argus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import xmlbeans.oasis.xacml.x2.x0.saml.assertion.XACMLPolicyQueryDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import de.fzj.unicore.wsrflite.xfire.WSRFClientFactory;
import eu.unicore.util.httpclient.IClientConfiguration;

public class ArgusPAPClient{
	private SAMLXACMLProvisioningInterface samlProxy;
	
	public ArgusPAPClient(URL address, int timeout, IClientConfiguration sec){
		String url=address.toString();
		try
		{
			sec.getHttpClientProperties().setConnectionTimeout(timeout);
			sec.getHttpClientProperties().setSocketTimeout(timeout);
			WSRFClientFactory factory = new WSRFClientFactory(sec);
			samlProxy = factory.createPlainWSProxy(SAMLXACMLProvisioningInterface.class, url);
		}catch(MalformedURLException m)
		{
			throw new IllegalArgumentException(m);
		}
	}

	public ResponseDocument sendRequest(XACMLPolicyQueryDocument req) throws IOException
	{
		try
		{
			ResponseDocument resp = samlProxy.policyRequest(req);
			if (resp == null)
				throw new IOException("Error during communication with Argus PAP server: didn't received SOAP answer (seems as Argus BUG)!");
			return resp;
		} catch (Exception e)
		{
			throw new IOException("Error during communication with Argus PAP server: ", e);
		}
		
	}

}

