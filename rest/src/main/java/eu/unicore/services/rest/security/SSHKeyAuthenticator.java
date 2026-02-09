package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.AuthenticationException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributeHolders;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributesHolder;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;
import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.restclient.sshkey.SSHUtils;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.BasicAttributeHolder;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Authenticate by checking tokens using the SSH public key(s) 
 * that are available for the user. The public keys are typically read 
 * from '~/.ssh/authorized_keys’ via UFTPD or TSI, but can be 
 * managed "manually" in a file, if desired. 
 * 
 * Supports JWT tokens and the homegrown tokens used by UFTP-Client/Authserver
 * 
 * @author schuller 
 */
public class SSHKeyAuthenticator implements IAuthenticator, KernelInjectable {

	private static final Logger logger = Log.getLogger(Log.SECURITY, SSHKeyAuthenticator.class);

	private String dnTemplate = "CN=%s, OU=ssh-local-users";

	private String identityAssign;

	private String file;
	private long updateInterval = 600;
	private boolean useAuthorizedKeys = true;
	private String userInfo;
	private String serverDN;

	private final static Collection<String> s = new ArrayList<>(); 
	static{
		s.add("JWT-SSH-KEY");
		s.add("UNICORE-SSH-KEY");
	}

	private UserPublicKeyCache keyCache = null;

	@Override
	public void setKernel(Kernel kernel){
		keyCache = UserPublicKeyCache.get(kernel);
		keyCache.setFile(file);
		keyCache.setUseAuthorizedKeys(useAuthorizedKeys);
		keyCache.setUpdateInterval(updateInterval);
		keyCache.setDnTemplate(dnTemplate);
		keyCache.setIdentityAssign(identityAssign);
		if(useAuthorizedKeys && userInfo!=null) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends UserInfoSource>userInfoClass = (Class<? extends UserInfoSource>)Class.forName(userInfo);
				keyCache.getUserInfoSources().add(kernel.load(userInfoClass));
			}catch(Exception ex) {
				throw new ConfigurationException("Could not load user info class <"+userInfo+">", ex);
			}
		}
		try {
			serverDN = kernel.getContainerSecurityConfiguration().getCredential().getSubjectName();
		}catch(Exception ex) {}
	}

	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	@Override
	public boolean authenticate(Message message, SecurityTokens tokens){
		Pair<String,String> authInfo = null;
		boolean haveCredentials = false;
		String bearerToken = CXFUtils.getBearerToken(message);
		if(bearerToken!=null) {
			haveCredentials = true;
			authInfo = authenticateJWT(bearerToken, tokens);
		}
		if(authInfo!=null){
			String requestedUserName = authInfo.getM1();
			String dn = authInfo.getM2();			
			logger.debug("{} --> <{}> {} (JWT)", requestedUserName, dn);
			tokens.setUserName(dn);
			tokens.setConsignorTrusted(true);
			tokens.getContext().put(AuthNHandler.USER_AUTHN_METHOD, "SSHKEY");
			storeAttributes(requestedUserName, tokens);
		}
		return haveCredentials;
	}

	private Pair<String,String> authenticateJWT(String bearerToken, SecurityTokens tokens) {
		try{
			JSONObject json = null;
			try{
				json = JWTUtils.getPayload(bearerToken);
			}catch(Exception e) {
				logger.debug("Not a JSON/JWT token");
				return null;
			}
			String exp = json.optString("exp", null);
			if(exp==null)throw new Exception("JWT Token does not specify a lifetime ('exp' attribute).");
			String subject = json.getString("sub");
			String issuer = json.getString("iss");
			if(!subject.equals(issuer)){
				// delegation - not handled here, but in AuthHandler.
				throw new IllegalStateException("Subject and issuer do not match.");
			}
			String dn = jwtTokenAuth(subject, bearerToken);
			if(dn!=null)return new Pair<>(subject, dn);
		}catch(Exception jtd){
			new AuthenticationException("Malformed Bearer token.", jtd);
		}
		return null;
	}

	private String jwtTokenAuth(String username, String jwtToken) throws IOException {
		AttributeHolders attr = keyCache.get(username);
		if(attr==null)return null;
		List<AttributesHolder>coll = attr.get();
		if(coll != null){
			for(AttributesHolder af : coll){
				if(af.getPublicKeys().isEmpty()){
					logger.error("Server config error: No public key stored for {}", username);
					continue;
				}
				for(String sshKey: af.getPublicKeys()) {
					try {
						JWTUtils.verifyJWTToken(jwtToken, SSHUtils.readPubkey(sshKey), serverDN);
						return af.getDN();
					}catch(Exception ae) {}
				}
			}
		}
		return null;
	}

	public void setFile(String fileName) {
		this.file = fileName;
	}

	public void setUseAuthorizedKeys(boolean useAuthorizedKeys) {
		this.useAuthorizedKeys = useAuthorizedKeys;
	}

	public void setUpdateInterval(String update) {
		updateInterval = Long.valueOf(update);
	}

	public void setDnTemplate(String dnTemplate) {
		this.dnTemplate = dnTemplate;
	}

	public void setIdentityAssign(String identityAssign) {
		this.identityAssign = identityAssign;
	}

	public void setUserInfo(String userInfo) {
		this.userInfo = userInfo;
	}

	// store attributes for AuthAttributesCollector to pick up
	private void storeAttributes(String requestedUser, SecurityTokens tokens){
		BasicAttributeHolder attr = new BasicAttributeHolder();
		attr.uid = requestedUser;
		tokens.getContext().put(AuthAttributesCollector.ATTRIBUTES, attr);
	}

	@Override
	public String toString(){
		return "SSH Keys [from:"+(file!=null?" <"+file+">":"")
				+(!useAuthorizedKeys?"":" <target system>")+" ]";
	}

}
