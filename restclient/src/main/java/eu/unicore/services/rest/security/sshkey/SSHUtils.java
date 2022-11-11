package eu.unicore.services.rest.security.sshkey;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.utils.Base64;

import com.hierynomus.sshj.key.KeyAlgorithm;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import net.schmizz.sshj.ConfigImpl;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.signature.Signature;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyFormat;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

/**
 * helpers to read SSH private/public keys
 * 
 * @author schuller
 */
public class SSHUtils {

	public static PrivateKey readPrivateKey(File priv, PasswordSupplier password) throws IOException {
		return getFileKeyProvider(priv, password).getPrivate();
	}
	
	public static PublicKey readPublicKey(File file) throws IOException, GeneralSecurityException {
		String pubkey = FileUtils.readFileToString(file, "UTF-8");
		return readPubkey(pubkey);
	}
	
	final static String[] ssh_options = { "from=", "no-", "environment=",
			"permitopen=", "principals=", "tunnel=",
			"ssh-rsa", "ssh-ed25519",
	};

	
	public static PublicKey readPubkey(String pubkey) throws IOException, GeneralSecurityException {
		StringTokenizer st = new StringTokenizer(pubkey);
		outer: while(st.hasMoreTokens()) {
			String token = st.nextToken();
			for(String opt: ssh_options){
				if(token.startsWith(opt))continue outer;
			}
			try{
				Buffer.PlainBuffer buf = new Buffer.PlainBuffer(Base64.decodeBase64(token.getBytes()));
				return buf.readPublicKey();
			}catch(Exception ex) {}
		}
		throw new GeneralSecurityException("Not recognized as public key: "+pubkey);
	}

	private static final ConfigImpl sshConfig = new DefaultConfig();

	private static FileKeyProvider getFileKeyProvider(File key, PasswordSupplier password) throws IOException {
		KeyFormat format = KeyProviderUtil.detectKeyFileFormat(key);
		FileKeyProvider fkp = Factory.Named.Util.create(sshConfig.getFileKeyProviderFactories(), format.toString());
		if (fkp == null) {
			throw new IOException("No support for " + format + " key file");
		}
		PasswordFinder passwordFinder = new PasswordFinder() {
			@Override
			public boolean shouldRetry(Resource<?> resource) {
				return false;
			}
			@Override
			public char[] reqPassword(Resource<?> resource) {
				return password.getPassword();
			}
		};
		fkp.init(key, passwordFinder);
		return fkp;
	}

	public static SSHKeyUC createAuthData(File key, PasswordSupplier pf, String token) throws GeneralSecurityException, IOException {
		SSHKeyUC authData = new SSHKeyUC();
		byte[] hashedToken = hash(token.getBytes());
		authData.token = new String(Base64.encodeBase64(hashedToken));
		PrivateKey pk = readPrivateKey(key, pf);

		final String kt = KeyType.fromKey(pk).toString();
		KeyAlgorithm ka = Factory.Named.Util.create(sshConfig.getKeyAlgorithms(), kt);
		Signature signature = ka.newSignature();
		if (signature == null) {
			throw new GeneralSecurityException("Could not create signature instance for " + kt + " key");
		}
		signature.initSign(pk);
		signature.update(hashedToken);
		byte[]signed = signature.sign();
		authData.signature = new String(Base64.encodeBase64(signed));
		return authData;
	}

	/**
	 * create a new authentication token
	 *
	 * @param key - file containing the private key
	 * @param token - plaintext token to hash and sign
	 */
	public static SSHKeyUC createAuthData(File key, char[] password, String token) throws GeneralSecurityException, IOException {
		SSHKeyUC authData = new SSHKeyUC();
		byte[] hashedToken = hash(token.getBytes());
		authData.token = new String(Base64.encodeBase64(hashedToken));
		PrivateKey pk = readPrivateKey(key,
				new PasswordSupplier() {
					@Override
					public char[] getPassword() {
						return	password;
					}
		});
		final String kt = KeyType.fromKey(pk).toString();
		KeyAlgorithm ka = Factory.Named.Util.create(sshConfig.getKeyAlgorithms(), kt);
		Signature signature = ka.newSignature();
		if (signature == null) {
			throw new GeneralSecurityException("Could not create signature instance for " + kt + " key");
		}
		signature.initSign(pk);
		signature.update(hashedToken);
		byte[]signed = signature.sign();
		if(kt.toLowerCase().contains("ecdsa")) {
			signed = encodeECDSA(signed);
		}
		authData.signature = new String(Base64.encodeBase64(signed));
		return authData;
	}

	/**
	 * cryptographic part of token validation: checks that
	 * the encrypted token can be decrypted correctly with the
	 * given public key
	 *
	 * @param authData
	 * @param pubkey - PEM formatted public key
	 */
	public static boolean validateAuthData(SSHKeyUC authData, String pubkey){
		if(authData == null || authData.token == null || authData.token.isEmpty()
				||  authData.signature == null || authData.signature.isEmpty()){
			return false;
		}
		try{
			PublicKey pub = SSHUtils.readPubkey(pubkey);
			final String kt = KeyType.fromKey(pub).toString();
			KeyAlgorithm ka = Factory.Named.Util.create(sshConfig.getKeyAlgorithms(), kt);
			Signature signature = ka.newSignature();
			if (signature == null) {
				throw new GeneralSecurityException("Could not create signature instance for " + pub + " key");
			}
			signature.initVerify(pub);
			byte[]token = Base64.decodeBase64(authData.token.getBytes());
			signature.update(token);
			byte[]sig = Base64.decodeBase64(authData.signature.getBytes());
			Buffer.PlainBuffer buf = new Buffer.PlainBuffer();
			buf.putString(kt);
			buf.putBytes(sig);
			return signature.verify(buf.getCompactData());
		}
		catch(Exception ex){
			return false;
		}
	}

	public static byte[] hash(byte[]data) throws GeneralSecurityException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(data);
		return md.digest();
	}

	private static byte[] encodeECDSA(byte[] sig) {
		int rIndex = 3;
		int rLen = sig[rIndex++] & 0xff;
		byte[] r = new byte[rLen];
		System.arraycopy(sig, rIndex, r, 0, r.length);

		int sIndex = rIndex + rLen + 1;
		int sLen = sig[sIndex++] & 0xff;
		byte[] s = new byte[sLen];
		System.arraycopy(sig, sIndex, s, 0, s.length);

		System.arraycopy(sig, 4, r, 0, rLen);
		System.arraycopy(sig, 6 + rLen, s, 0, sLen);

		Buffer.PlainBuffer buf = new Buffer.PlainBuffer();
		buf.putMPInt(new BigInteger(r));
		buf.putMPInt(new BigInteger(s));

		return buf.getCompactData();
	}

}
