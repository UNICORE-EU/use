package eu.unicore.services.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.AuthenticationException;
import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributeHolders;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributesHolder;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.services.rest.security.sshkey.SSHKeyUC;
import eu.unicore.services.rest.security.sshkey.SSHUtils;
import eu.unicore.services.security.AuthAttributesCollector;
import eu.unicore.services.security.AuthAttributesCollector.PAMAttributes;
import eu.unicore.services.utils.Pair;
import eu.unicore.util.Log;
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
		HttpServletRequest request = CXFUtils.getServletRequest(message);
		Pair<String,String> authInfo = null;
		boolean jwtMode = true;
		boolean haveCredentials = false;
		if(request.getHeader(SSHKeyUC.HEADER_PLAINTEXT_TOKEN)==null){
			String bearerToken = CXFUtils.getBearerToken(message);
			if(bearerToken!=null) {
				haveCredentials = true;
				authInfo = authenticateJWT(bearerToken, tokens);
			}
		}
		else {
			haveCredentials = true;
			authInfo = authenticateLegacy(message, tokens);
			jwtMode = false;
		}
		if(authInfo!=null){
			String requestedUserName = authInfo.getM1();
			String dn = authInfo.getM2();			
			logger.debug("{} --> <{}> {}", requestedUserName, dn, jwtMode? "(JWT)": "(proprietary token)");
			tokens.setUserName(dn);
			tokens.setConsignorTrusted(true);
			tokens.getContext().put(AuthNHandler.USER_AUTHN_METHOD, "SSHKEY");
			storeAttributes(requestedUserName, tokens);
		}
		return haveCredentials;
	}

	private Pair<String,String> authenticateLegacy(Message message, SecurityTokens tokens) {
		HTTPAuthNTokens auth = (HTTPAuthNTokens)tokens.getContext().get(SecurityTokens.CTX_LOGIN_HTTP);
		if(auth == null){
			auth = CXFUtils.getHTTPCredentials(message);
			if(auth!=null)tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP,auth);
		}
		if(auth != null && auth.getUserName()!=null) {
			SSHKeyUC authData = new SSHKeyUC();
			authData.username = auth.getUserName();
			authData.signature = auth.getPasswd();
			HttpServletRequest request = CXFUtils.getServletRequest(message);
			authData.token = request.getHeader(SSHKeyUC.HEADER_PLAINTEXT_TOKEN);
			try{
				String dn = sshKeyAuth(authData);
				if(dn!=null)return new Pair<>(authData.username, dn);
			}catch(IOException e) {
				throw new AuthenticationException("Could not validate SSH token for "+auth.getUserName());
			}
		}
		else if(auth.getUserName()!=null) {
				logger.debug("No match found for {}", auth.getUserName());
		}
		return null;
	}

	private String sshKeyAuth(SSHKeyUC authData) throws IOException {
		String username = authData.username;
		AttributeHolders attr = keyCache.get(username);
		if(attr==null)return null;
		List<AttributesHolder>coll = attr.get();
		if(coll != null){
			for(AttributesHolder af : coll){
				if(af.sshkey==null || af.sshkey.isEmpty()){
					logger.error("Server config error: No public key stored for {}", username);
					continue;
				}
				if(SSHUtils.validateAuthData(authData,af.sshkey)){
					return af.dn;
				}
			}
		}
		return null;
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
			if(exp==null)throw new AuthenticationException("JWT Token does not specify a lifetime ('exp' attribute).");
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
	
	private String jwtTokenAuth(String username, String jwtToken) throws Exception {
		AttributeHolders attr = keyCache.get(username);
		if(attr==null)return null;
		List<AttributesHolder>coll = attr.get();
		if(coll != null){
			for(AttributesHolder af : coll){
				if(af.sshkey==null || af.sshkey.isEmpty()){
					logger.error("Server config error: No public key stored for {}", username);
					continue;
				}
				JWTUtils.verifyJWTToken(jwtToken, SSHUtils.readPubkey(af.sshkey), serverDN);
				return af.dn;
			}
		}
		return null;
	}

	public void setFile(String fileName) {
		this.file = fileName;
	}

	public String getFile() {
		return file;
	}

	public void setUseAuthorizedKeys(boolean useAuthorizedKeys) {
		this.useAuthorizedKeys = useAuthorizedKeys;
	}

	public void setUpdateInterval(String update) {
		try{
			updateInterval = Long.valueOf(update);
		}
		catch(Exception ex){}
	}

	public void setDnTemplate(String dnTemplate) {
		this.dnTemplate = dnTemplate;
	}

	public void setUserInfo(String userInfo) {
		this.userInfo = userInfo;
	}

	// store attributes for AuthAttributesCollector to pick up
	private void storeAttributes(String requestedUser, SecurityTokens tokens){
		PAMAttributes attr = new PAMAttributes();
		attr.uid = requestedUser;
		tokens.getContext().put(AuthAttributesCollector.ATTRIBUTES, attr);
	}
	
	public String toString(){
		return "SSH Keys [from:"+(file!=null?" <"+file+">":"")
				+(!useAuthorizedKeys?"":" <target system>")+" ]";
	}

}
