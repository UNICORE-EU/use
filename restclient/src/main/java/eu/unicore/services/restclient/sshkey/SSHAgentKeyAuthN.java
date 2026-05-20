package eu.unicore.services.restclient.sshkey;

import java.io.IOException;

import org.apache.hc.core5.http.HttpMessage;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.jwt.JWTUtils;

/**
 * authenticate with a JWT token signed with a private key (SSH key), getting
 * the signature from the SSH agent
 * 
 * @author schuller
 */
public class SSHAgentKeyAuthN implements IAuthCallback {

	private final SSHAgent agent;
	private final String user;
	private final long lifetime; 

	private String token;

	private long issued; 

	public SSHAgentKeyAuthN(String user, SSHAgent agent){
		this(user, 300, agent);
	}

	public SSHAgentKeyAuthN(String user, long lifetime, SSHAgent agent){
		this.user = user;
		this.lifetime = lifetime;
		this.agent = agent;
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		httpMessage.removeHeaders("Authorization");
		httpMessage.addHeader("Authorization", "Bearer "+getToken());
	}

	public String getToken() throws Exception {
		if(token == null || !tokenStillValid()) {
			createToken();
		}
		return token;
	}

	protected void createToken() throws Exception {
		JWTClaimsSet claimsSet = JWTUtils.buildClaimsSet(user, lifetime, user, null);
		JWSSigner signer = agent.getSigner();
		JWSAlgorithm alg = signer.supportedJWSAlgorithms().iterator().next();
		JWSHeader header = new JWSHeader(alg);
		SignedJWT jwt = new SignedJWT(header, claimsSet);
		jwt.sign(signer);
		issued = System.currentTimeMillis();
		token = jwt.serialize();
	}

	protected boolean tokenStillValid() throws IOException {
		return issued+(500*lifetime)>System.currentTimeMillis();
	}

}
