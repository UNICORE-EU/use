package eu.unicore.services.restclient.sshkey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
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

/**
 * support for ssh-agent
 *
 * @author schuller
 */
public class SSHAgent {

	private SSHAgentProxy ap ;

	private String keyFile;

	private static boolean verbose = false;

	Identity id = null;

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

	public void setVerbose(boolean verboseS) {
		verbose = verboseS;
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
		if(description.contains("ssh-dss")){
			try{
				signature = dsa_convertToDER(signature);
			}
			catch(IOException e){
				throw new GeneralSecurityException(e);
			}
		}
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

	private void assertIdentity() throws IOException, GeneralSecurityException {
		if(id==null) {
			Identity[] ids = ap.getIdentities();
			if(ids.length>1 && verbose) {
				System.err.println("NOTE: more than one identity in SSH agent -"
						+ " you might want to use '--identity <path_to_private_key>'");
			}
			if(ids.length==0)throw new GeneralSecurityException("No identities loaded in SSH agent!");
			id = ids[0];
		}
	}
	// signature DSA format
	private byte[] dsa_convertToDER(byte[] rawSignature) throws IOException {
		byte[] val = new byte[20];
		System.arraycopy(rawSignature, 0, val, 0, 20);
		BigInteger r = new BigInteger(val);
		System.arraycopy(rawSignature, 20, val, 0, 20);
		BigInteger s = new BigInteger(val);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ASN1OutputStream os = ASN1OutputStream.create(bos, ASN1Encoding.DER);
		DERSequence seq = new DERSequence(new ASN1Integer[]{new ASN1Integer(r),new ASN1Integer(s)});
		os.writeObject(seq);
		os.close();
		return bos.toByteArray();
	}

	public static boolean isAgentAvailable(){
		if(Boolean.parseBoolean(SSHUtils.getSystemProperty("UNICORE_NO_SSH_AGENT", "false"))){
			if(verbose) {
				System.err.println("Agent DISABLED via environment setting 'UNICORE_NO_SSH_AGENT'");
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
