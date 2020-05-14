package eu.unicore.services.rest.security;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;

import eu.unicore.security.HTTPAuthNTokens;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.util.Log;

/**
 * @author mgolik
 * @author jrybicki
 * @author schuller 
 */
public class FilebasedAuthenticator implements IAuthenticator {

	private static final Logger logger =  Log.getLogger(Log.SECURITY,FilebasedAuthenticator.class);
	
	private final Map<String,AttributesHolder>db = new HashMap<String,AttributesHolder>();
	
	private File dbFile;
	private long lastUpdated;
	private String file;
	
	public void setFile(String fileName) {
		this.file = fileName;
		dbFile = new File(file);
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
			if(logger.isDebugEnabled()){
				logger.debug("Authenticated via local username/password: <"+dn+">");
			}
		}
		return true;
	}
	
	public String toString(){
		return "Username/password ["+dbFile+"]";
	}
	
	private synchronized void updateDB() throws IOException {
		if(lastUpdated == 0 || dbFile.lastModified() > lastUpdated){
			logger.info("(Re)reading username/password authentication info from <"+dbFile.getAbsolutePath()+">");
			lastUpdated = dbFile.lastModified();
			try(BufferedReader bufferedReader = new BufferedReader(new FileReader(dbFile))){
				String line;
				while((line = bufferedReader.readLine())!=null) {
					if (line.trim().startsWith("#") || line.trim().isEmpty()) {
						continue;
					}
					try{
						AttributesHolder af = new AttributesHolder(line);
						db.put(af.user,af);
					}
					catch(IllegalArgumentException ex){
						logger.error("Invalid line in user db "+dbFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
	private String usernamePassword(String username, String password) {
		String dn=null;
		AttributesHolder af = db.get(username);
		if(af == null){
			return null;
		}
		if(verifyPass(password,af.hash,af.salt)){
			dn=af.dn;
		}
		return dn;
	}
	
	public static void main(String[] args) throws Exception {
		Console console = System.console();
		console.printf("Generate line for the username/password file\n");
		String username = console.readLine("Username:");
		String password = new String(console.readPassword("Password:"));
		String dn = new String(console.readPassword("DN:"));
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
			String pashHash = generatePassHash(pass, salt);
			return hash.equals(pashHash);
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Unable to generate hash", ex);
			return false;
		}
	}

	private static String getSalt() throws NoSuchAlgorithmException, NoSuchProviderException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		String saltString = convertBytesToString(salt).replaceAll(":", "|");
		return saltString;
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
		
		public AttributesHolder(String line) throws IllegalArgumentException {
			String[] fields = line.split(":",4);
			//#user:hash:salt
			if (fields.length!=4) {
				logger.error("Invalid line:"+line);
				throw new IllegalArgumentException();
			}
			user=fields[0];
			hash=fields[1];
			salt=fields[2];
			dn=fields[3];
		}
	}

}
