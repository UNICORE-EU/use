package eu.unicore.services.rest.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.KernelInjectable;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.util.Log;

/**
 * Authenticate the user via her X.509 credential, if present.
 * The X.509 credential can be obtained in two ways, depending on whether
 * a Gateway is configured or not.
 * <ul>
 * <li>without a Gateway, the X.509 chain from the transport layer is used</li>
 * <li>with a Gateway, the value of the special HTTP header "X-UNICORE-Consignor" is used</li>
 * </ul> 
 *
 * @author schuller 
 */
public class X509Authenticator implements IAuthenticator, KernelInjectable  {

	private static final Logger logger =  Log.getLogger(Log.SECURITY,X509Authenticator.class);
	
	private Kernel kernel;

	// validate Gateway signature on the Consignor header
	private boolean validate = true;
	
	private boolean haveGateway = true;
	
	@Override
	public final Collection<String>getAuthSchemes(){
		// nothing here for the WWW-Authenticate header
		return Collections.emptySet();
	}

	public void setValidate(boolean validate) {
		this.validate = validate;
	}
	
	public void setKernel(Kernel kernel){
		this.kernel = kernel;
		try {
			haveGateway = kernel.getContainerSecurityConfiguration().isGatewayAuthnEnabled();
		}catch(Exception e) {}
	}
	
	@Override
	public boolean authenticate(Message message, SecurityTokens tokens) {
		if(haveGateway){
			return extractFromGWHeader(message, tokens);
		}
		else{
			return extractFromTLS(message, tokens);
		}
	}

	protected boolean extractFromTLS(Message message, SecurityTokens tokens){
		X509Certificate[] certs = CXFUtils.getSSLCerts(message);
		if(certs == null)return false;
		X509Certificate cert = certs[0];
		String dn = cert.getSubjectX500Principal().getName();
		tokens.setUser(certs);
		tokens.setUserName(dn);
		tokens.setConsignor(certs);
		tokens.setConsignorTrusted(true);
		if(logger.isDebugEnabled()){
			logger.debug("Authenticated X.509 certificate (TLS): <"+dn+">");
		}
		return true;
	}
	
	protected boolean extractFromGWHeader(Message message, SecurityTokens tokens){
		HttpServletRequest req =(HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (req == null)return false;
		String consignerInfo = req.getHeader("X-UNICORE-Consignor");
		if(consignerInfo==null)return false;
		String dn = null;
		String dsig = null;
		NameValuePair[] parsed = BasicHeaderValueParser.parseParameters(consignerInfo, new BasicHeaderValueParser());
		for(NameValuePair p: parsed){
			if("DN".equals(p.getName()))dn=p.getValue();
			if("DSIG".equals(p.getName()))dsig=p.getValue();
		}
		if(dn==null||dsig==null)return false;
		
		if(validate && !isValid(dn,dsig)) {
			return true;
		}
		tokens.setUserName(dn);
		tokens.setConsignorTrusted(true);
		if(logger.isDebugEnabled()){
			logger.debug("Authenticated X.509 certificate (via Gateway): <"+dn+">");
		}
		return true;
	}
	
	private boolean isValid(String dn, String signature){
		try{
			PublicKey pub = kernel.getContainerSecurityConfiguration().getGatewayCertificate().getPublicKey();
			String alg = "RSA".equalsIgnoreCase(pub.getAlgorithm())? "SHA1withRSA" : "SHA1withDSA";
			Signature dsig = Signature.getInstance(alg);
			dsig.initVerify(pub);
			byte[]token = hash(dn.getBytes());
			dsig.update(token);
			byte[]sig = Base64.decodeBase64(signature.getBytes());
			boolean res = dsig.verify(sig);
			if(!res && logger.isDebugEnabled()){
				logger.debug("Got invalid signature for DN <"+dn+">");
			}
			return res;
		}
		catch(Exception ex){
			Log.logException("Error verifying signature",ex,logger);
			return false;
		}
	}
	
	private byte[] hash(byte[]data) throws GeneralSecurityException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(data);
		return md.digest();
	}
	
	public String toString() {
		return "X509 ["+(haveGateway?"extract DN from Gateway header":"extract DN from SSL peer")+"]";
	}
	
}
