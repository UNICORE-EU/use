package eu.unicore.services.rest.security.sshkey;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

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
	
	public static PublicKey readPubkey(String pubkey) throws IOException, GeneralSecurityException {
		StringTokenizer st = new StringTokenizer(pubkey);
		try {
			st.nextToken(); // format
			String base64 = st.nextToken();
			try{
				st.nextToken(); // optional comment
			}catch(NoSuchElementException e){/*ignored*/}
			return new Buffer.PlainBuffer(Base64.decodeBase64(base64.getBytes())).readPublicKey();
		} catch (NoSuchElementException e) {
			throw new IllegalArgumentException("Cannot read public key, expect SSH format");
		}
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
}
