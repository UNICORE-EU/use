package eu.unicore.services.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.AuthenticationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.SignatureStatus;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.util.Log;

/**
 * if the requested action requires it, check whether we have
 * a valid signature 
 */
public class DSignCheck {

	private static final Logger logger=Log.getLogger(Log.SECURITY,DSignCheck.class);

	private final List<String>actionsRequiringSignatures=new ArrayList<String>();
	
	private boolean dsigCheckingEnabled;
	
	public DSignCheck(boolean dsigCheckingEnabled) {
		logger.debug("Initialise checking of digital signature status.");
		this.dsigCheckingEnabled = dsigCheckingEnabled;
	}
	
	/**
	 * add SOAP actions to the special list requiring signed messages.
	 * Actually any string can be used for identification (e.g. web method name),
	 * not only SOAP actions.
	 * Names are stored and verified using needSignature method.
	 * @param actions
	 */
	public void addSOAPActionsRequiringSignatures(String ... actions){
		actionsRequiringSignatures.addAll(Arrays.asList(actions));
	}
	
	/**
	 * Returns true if the action argument is in the set of actions requiring signatures.
	 * Returns false otherwise. 
	 * @param action action name or null
	 * @return false for null, true/false if arg is not null.
	 */
	public boolean needSignature(String action){
		if (action==null)
			return false;
		if (!dsigCheckingEnabled)
			return false;
		boolean b=actionsRequiringSignatures.contains(action);
		logger.debug("Checking signatures for <"+action+"> = "+b);
		return b;
	}
	
	/**
	 * <ul>
	 * <li>If signature is present and invalid - failure </li>
	 * <li>If the signature is present and fully valid - success </li>
	 * <li>Otherwise check if the signature is required either for soap action
	 * or for the service/method pair. If yes then fail. Otherwise succeed. </li>
	 * </ul>
	 */
	public void checkDigitalSignature(SecurityTokens tokens, String action,
			ResourceDescriptor d) throws AuthenticationException {
		
		String method = null;
		if (d != null && d.getServiceName() != null && action != null)
			method = d.getServiceName() + "." + action;
		
		String soapAction=(String)tokens.getContext().get(SecurityTokens.CTX_SOAP_ACTION);

		if(SignatureStatus.WRONG.equals(tokens.getMessageSignatureStatus())){
			String msg="Non repudiation/integrity check failed on <"+
					d.toString()+">: digital signature is present but INVALID";
			logger.info(msg);
			throw new AuthenticationException(msg);
		}
		
		if(SignatureStatus.OK.equals(tokens.getMessageSignatureStatus())){
			logger.debug("Non repudiation/integrity check was SUCCESSFUL on <"+
					d.toString()+">");
			return;
		}
		
		logger.debug("Checking whether signature verification is mandatory " +
				"for invocation of [soap action=<"+soapAction+"> or method=<" 
				+ method + ">]");
		
		if (!needSignature(soapAction) && !needSignature(method)) 
			return;
		
		logger.debug("Signature verification is mandatory for invocation of " +
				"[soap action=<"+soapAction+"> or method=<" + method + ">]");
		
		String msg="Non repudiation/integrity check failed on <"+d.toString()+">: signature is required for <"+action+">";
		logger.info(msg);
		throw new AuthenticationException(msg);
	}

}
