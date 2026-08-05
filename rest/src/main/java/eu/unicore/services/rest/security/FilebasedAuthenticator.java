package eu.unicore.services.rest.security;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.cxf.message.Message;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.util.Log;

/**
 * @author mgolik
 * @author jrybicki
 * @author schuller 
 */
public class FilebasedAuthenticator implements IAuthenticator, IAuthenticator.Settable {

	private static final Logger logger =  Log.getLogger(Log.SECURITY,FilebasedAuthenticator.class);

	private final Map<String,AttributesHolder> db = new LinkedHashMap<>();

	private File dbFile;
	private long lastUpdated;
	private String file;
	private boolean immutable = false;

	public void setFile(String fileName) {
		this.file = fileName;
		this.dbFile = new File(file);
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	public String getFile() {
		return file;
	}

	private final static Collection<String> s = Collections.singletonList("Basic");

	@Override
	public final Collection<String>getAuthSchemes(){
		return s;
	}

	@Override
	public boolean authenticate(Message message, SecurityTokens tokens) {
		HTTPAuthNTokens http = (HTTPAuthNTokens)tokens.getContext().get(SecurityTokens.CTX_LOGIN_HTTP);
		if(http == null){
			http = CXFUtils.getHTTPCredentials(message);
			tokens.getContext().put(SecurityTokens.CTX_LOGIN_HTTP,http);
		}
		if(http == null)return false;
		try{
			updateDB();
		}catch(IOException ioe){
			throw new RuntimeException("Server error: could not update user database.", ioe);
		}
		String dn = usernamePassword(http.getUserName(), http.getPasswd());
		if(dn != null){
			tokens.setUserName(dn);
			tokens.setConsignorTrusted(true);
			tokens.getContext().put(AuthNHandler.USER_AUTHN_METHOD, authMethod);
			logger.debug("Authenticated via local username/password: <{}>", dn);
		}
		return true;
	}

	private static final String authMethod = "PASSWORD_FILE";

	@Override
	public String getAuthMethod(){
		return authMethod;
	}

	@Override
	public boolean set(SecurityTokens tokens, String password) throws Exception {
		if(immutable)return false;
		String dn = null;
		// validate first
		HTTPAuthNTokens http = (HTTPAuthNTokens)tokens.getContext().get(SecurityTokens.CTX_LOGIN_HTTP);
		if(http != null) {
			dn = usernamePassword(http.getUserName(), http.getPasswd());
		}
		if(dn==null) {
			throw new SecurityException("Not authenticated.");
		}
		try {
			String username = http.getUserName();
			String line = generateLine(username, password, dn);
			AttributesHolder ah = new AttributesHolder(line);
			_lock.lock();
			db.put(username, ah);
			var lines = readLines(); 
			for(int i = 0; i<lines.size(); i++) {
				String l = lines.get(i);
				if(l.startsWith(username+":")) {
					lines.set(i, line);
				}
			}
			writeFile(lines);
		}
		finally {
			_lock.unlock();
		}
		return true;
	}

	@Override
	public String toString(){
		return "Username/password ["+dbFile+"]";
	}

	private Lock _lock = new ReentrantLock();

	private synchronized void updateDB() throws IOException {
		if(lastUpdated == 0 || dbFile.lastModified() > lastUpdated){
			try {
				_lock.lock();
				logger.info("(Re)reading username/password authentication info from <"+dbFile.getAbsolutePath()+">");
				lastUpdated = dbFile.lastModified();
				for(String line: readLines()) {
					if (line.trim().startsWith("#") || line.trim().isEmpty()) {
						continue;
					}
					try{
						AttributesHolder af = new AttributesHolder(line);
						db.put(af.user,af);
					}
					catch(Exception ex){
						logger.error("Invalid line in user authfile {}: {}", dbFile.getAbsolutePath(), line);
					}
				}
			}finally {
				_lock.unlock();
			}
		}
	}

	List<String>readLines() throws IOException {
		List<String>lines = new ArrayList<>();
		try(BufferedReader bufferedReader = new BufferedReader(new FileReader(dbFile))){
			String line;
			while((line = bufferedReader.readLine())!=null) {
				lines.add(line);
			}
		}
		return lines;
	}

	void writeFile(List<String> lines) throws IOException {
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))){
			for(String line: lines) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	private String usernamePassword(String username, String password) {
		AttributesHolder af = db.get(username);
		if(af == null){
			return null;
		}
		return verifyPass(password,af.hash,af.salt) ? af.dn : null;
	}

	public static void main(String[] args) throws Exception {
		Console console = System.console();
		console.printf("Generate line for the username/password file\n");
		String username = console.readLine("Username:");
		String password = new String(console.readPassword("Password:"));
		String dn = new String(console.readLine("DN:"));
		System.out.println("Add following line to password file");
		System.out.printf(generateLine(username,password,dn));
	}

	public static String generateLine(String username,String password,String dn) throws Exception {
		boolean havePassword = !password.isEmpty();
		String salt = getSalt();
		String hash = havePassword?generatePassHash(password, salt):"";
		return String.format("%s:%s:%s:%s\n",username,hash,salt,dn);
	}

	private boolean verifyPass(String pass, String hash, String salt) {
		try {
			return hash.equals(generatePassHash(pass, salt));
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Unable to generate hash", ex);
			return false;
		}
	}

	private static String getSalt() throws NoSuchAlgorithmException, NoSuchProviderException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return convertBytesToString(salt).replaceAll(":", "|");
	}

	private static String convertBytesToString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	private static String generatePassHash(String passwordToHash, String salt) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(salt.getBytes());
		byte[] bytes = md.digest(passwordToHash.getBytes());
		return convertBytesToString(bytes);
	}

	public static class AttributesHolder {

		public final String user;
		public final String hash;
		public final String salt;
		public final String dn;

		public AttributesHolder(String line) {
			String[] fields = line.split(":",4);
			//#user:hash:salt:dn
			user=fields[0];
			hash=fields[1];
			salt=fields[2];
			dn=fields[3];
		}
	}

}
