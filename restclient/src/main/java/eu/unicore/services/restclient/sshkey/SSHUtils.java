package eu.unicore.services.restclient.sshkey;

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

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import net.schmizz.sshj.ConfigImpl;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.Factory;
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

	public static byte[] hash(byte[]data) throws GeneralSecurityException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(data);
		return md.digest();
	}


}
