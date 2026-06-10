package eu.unicore.services.restclient.sshkey;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.Arrays;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.impl.EdDSAProvider;
import com.nimbusds.jose.crypto.impl.RSASSAProvider;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;

import eu.unicore.services.restclient.sshkey.SSHAgentProxy.Identity;
import eu.unicore.services.restclient.utils.UserLogger;

/**
 * support for ssh-agent
 *
 * @author schuller
 */
public class SSHAgent {

	private SSHAgentProxy ap ;

	private String keyFile;

	Identity id = null;

	static UserLogger logger = null;

	public SSHAgent() throws Exception {
		this(SSHAgentProxy.get());
	}

	SSHAgent(SSHAgentProxy ap) throws Exception {
		this.ap = ap;
		if(!ap.isAvailable()) {
			throw new IOException("SSH-Agent is not available");
		}
	}
	
	/**
	 * choose the identity - if not available in the agent, an exception is thrown
	 *
	 * @param keyFile
	 * @throws IOException
	 */
	public void selectIdentity(String keyFile) throws IOException {
		this.keyFile = keyFile;
		Identity[] ids = ap.getIdentities();
		if(ids.length==0)throw new IOException("No identities loaded in SSH agent!");
		id = ids[doSelectIdentity(ids)];
	}


	// get the "intended" identity from the agent
	private int doSelectIdentity(Identity[]ids) throws IOException {
		String pubkey = FileUtils.readFileToString(new File(keyFile+".pub"), "UTF-8");
		StringTokenizer st = new StringTokenizer(pubkey);
		st.nextToken(); // ignored
		String base64 = st.nextToken();
		byte[] bytes = Base64.getDecoder().decode(base64);

		for(int i=0; i<ids.length; i++) {
			Identity id = ids[i];
			if(Arrays.areEqual(bytes, id.getBlob())){
				return i;
			}
		}
		throw new IOException("No matching identity found in agent");
	}

	public void setLogger(UserLogger log) {
		logger = log;
	}

	/**
	 * create signature for the given data
	 * @param data - data to sign
	 * @return signature (only the actual signature data without any headers)
	 * @throws GeneralSecurityException
	 */
	public byte[] sign(byte[] data) throws GeneralSecurityException, IOException {
		assertIdentity();
		byte[] blob = id.getBlob();
		byte[] rawSignature = ap.sign(blob, data);
		String description = getSignatureAlgorithm(rawSignature);
		int offset = 8 + description.length();
		byte[] signature = new byte[rawSignature.length-offset];
		System.arraycopy(rawSignature, offset, signature, 0, signature.length);
		return signature;
	}

	private String getSignatureAlgorithm(byte[]signature) throws IOException, GeneralSecurityException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(signature));
		int len = dis.readInt();
		return new String(dis.readNBytes(len));
	}

	public String getAlgorithm() throws IOException, GeneralSecurityException {
		assertIdentity();
		return getAlgorithm(id);
	}

	private String getAlgorithm(Identity identity) throws IOException{
		byte[] blob = identity.getBlob();
		byte[] rawSignature = ap.sign(blob, "test123".getBytes());
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rawSignature));
		int len = dis.readInt();
		return new String(dis.readNBytes(len));
	}

	void assertIdentity() throws IOException, GeneralSecurityException {
		if(id==null) {
			Identity[] ids = ap.getIdentities();
			if(ids.length>1 && logger!=null) {
				logger.verbose("NOTE: more than one identity is available in the SSH agent - using the first one.");
			}
			if(ids.length==0)throw new GeneralSecurityException("No identities loaded in SSH agent!");
			id = ids[0];
		}
	}

	public static boolean isAgentAvailable(){
		if(Boolean.parseBoolean(SSHUtils.getSystemProperty("UNICORE_NO_SSH_AGENT", "false"))){
			if(logger!=null) {
				logger.verbose("Agent DISABLED via environment setting 'UNICORE_NO_SSH_AGENT'");
			}
			return false;
		}
		return SSHAgentProxy.get().isAvailable();
	}

	public JWSSigner getSigner() throws IOException, GeneralSecurityException {
		final Set<JWSAlgorithm> algorithms;
		String algo = getAlgorithm();
		if(algo.contains("ssh-ed25519")) {
			algorithms = EdDSAProvider.SUPPORTED_ALGORITHMS;
		}else if(algo.contains("rsa")){
			algorithms = RSASSAProvider.SUPPORTED_ALGORITHMS;
		}
		else throw new GeneralSecurityException("Unsupported SSH key signature type: "+algo);

		return new JWSSigner() {

			@Override
			public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
				try {
					return Base64URL.encode(SSHAgent.this.sign(signingInput));
				} catch (Exception e) {
					throw new JOSEException(e.getMessage(), e);
				}
			}

			@Override
			public Set<JWSAlgorithm> supportedJWSAlgorithms() {
				return algorithms;
			}

			private final JCAContext jcaContext = new JCAContext();

			@Override
			public JCAContext getJCAContext() {
				return jcaContext;
			}
		};
	}
}
