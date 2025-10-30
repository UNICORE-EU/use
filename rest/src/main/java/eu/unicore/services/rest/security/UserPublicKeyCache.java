package eu.unicore.services.rest.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.restclient.sshkey.SSHUtils;
import eu.unicore.util.Log;

/**
 * stores user public keys and associated user attributes read from a file
 * and/or some external system (such as UFTPD or TSI)
 *
 * @author schuller
 */
public class UserPublicKeyCache {

	private static final Logger logger = Log.getLogger(Log.SECURITY, UserPublicKeyCache.class);

	private final Map<String,AttributeHolders>db = new HashMap<>();

	private File dbFile = null;
	private boolean useAuthorizedKeys = true;
	private static long updateInterval = 600;
	private long lastUpdated;
	private String identityAssign;

	private String dnTemplate = "CN=%s, OU=ssh-local-users";

	private final Collection <UserInfoSource>sources = new HashSet<>();

	public static synchronized UserPublicKeyCache get(Kernel kernel){
		UserPublicKeyCache pc = kernel.getAttribute(UserPublicKeyCache.class);
		if(pc==null){
			pc = new UserPublicKeyCache();
			kernel.setAttribute(UserPublicKeyCache.class, pc);
		}
		return pc;
	}

	public AttributeHolders get(String username) throws IOException {
		readKeysFromServer(username);
		return getMappings().get(username);
	}

	private Map<String,AttributeHolders>getMappings() throws IOException {
		updateDB();
		return Collections.unmodifiableMap(db);
	}

	public void setFile(String fileName) {
		if(fileName!=null)this.dbFile = new File(fileName);
	}

	public void setUseAuthorizedKeys(boolean useAuthorizedKeys) {
		this.useAuthorizedKeys = useAuthorizedKeys;
	}

	public void setUpdateInterval(long update) {
		updateInterval = update;
	}

	public void setDnTemplate(String dnTemplate) {
		this.dnTemplate = dnTemplate;
	}

	public void setIdentityAssign(String identityAssign) {
		this.identityAssign = identityAssign;
	}

	public Collection <UserInfoSource> getUserInfoSources() {
		return sources;
	}

	protected synchronized void updateDB() throws IOException {
		if(dbFile==null)return;
		if(lastUpdated == 0 || dbFile.lastModified() > lastUpdated){
			logger.info("(Re)reading user keys from <{}>", dbFile.getAbsolutePath());
			lastUpdated = dbFile.lastModified();
			removeFileEntriesFromDB();
			try(BufferedReader bufferedReader = new BufferedReader(new FileReader(dbFile))) {
				String line;
				while((line = bufferedReader.readLine())!=null) {
					if (line.startsWith("#") || line.trim().isEmpty()) {
						continue;
					}
					try{
						AttributesHolder af = AttributesHolder.fromFileEntry(line);
						getOrCreateAttributes(af.user).add(af);
					}
					catch(IllegalArgumentException ex){
						logger.error("Invalid line in user keys file {}", dbFile.getAbsolutePath());
					}
				}
			}
		}
	}

	public synchronized void readKeysFromServer(String requestedUserName) {
		if(!useAuthorizedKeys)return;
		AttributeHolders attrs = getOrCreateAttributes(requestedUserName);
		if(!attrs.wantUpdate())return;
		Collection<AttributesHolder> updatedKeys = new ArrayList<>();
		boolean ok = true;
		for(UserInfoSource lServer: sources){
			try{
				AttributesHolder ah = lServer.getAttributes(requestedUserName, identityAssign);
				Collection<String> response = ah.getPublicKeys();
				String dn = ah.getDN();
				if(dn==null) {
					dn = String.format(dnTemplate, requestedUserName);
				}
				if(response!=null)parseUserInfo(response, requestedUserName, dn, updatedKeys);
			}
			catch(Exception ex){
				logger.debug("Could not get info for user <{}>: {}", requestedUserName, ex);
				ok = false;
			}
		}
		if(ok) {
			logger.debug("Have <{}> valid public keys for <{}>", updatedKeys.size(), requestedUserName);
			attrs.mergeKeysFromServer(updatedKeys);
			attrs.refresh();
		}
		else attrs.invalidate();
	}

	protected synchronized AttributeHolders getOrCreateAttributes(String user){
		AttributeHolders attr = db.get(user);
		if(attr == null){
			attr = new AttributeHolders();
			db.put(user,attr);
		}
		return attr;
	}

	protected void parseUserInfo(Collection<String> keys, String user, String dn, Collection<AttributesHolder> attrs) throws IOException {
		for(String key: keys){
			if(!hasEntry(attrs,key)){
				if(isValidKey(key)){
					AttributesHolder ah = new AttributesHolder(user,key,dn);
					ah.fromFile = false;
					attrs.add(ah);
					logger.info("Added SSH pub key for userID '{}' mapped to <{}>", user, dn);
				}
			}
		}
	}

	private boolean hasEntry(Collection<AttributesHolder> attrs, String key){
		for(AttributesHolder ah: attrs){
			if(ah.getPublicKeys().contains(key))return true;
		}
		return false;
	}

	public void removeFileEntriesFromDB(){
		for(AttributeHolders entry: db.values()){
			entry.removeFileEntries();
		}
	}

	public static class AttributesHolder {

		public final String user;
		public final List<String> publicKeys = new ArrayList<>();
		private String dn;
		public boolean fromFile = true;

		public AttributesHolder(String user){
			this.user = user;
		}

		private AttributesHolder(String user, String sshkey, String dn){
			this(user);
			publicKeys.add(sshkey);
			this.dn = dn;
		}

		public List<String> getPublicKeys(){
			return publicKeys;
		}

		public String getDN() {
			return dn;
		}

		public static AttributesHolder fromFileEntry(String line) throws IllegalArgumentException {
			String[] fields = line.split(":");
			//#user:sshkey:dn
			if (fields.length!=3) {
				logger.error("Invalid line: {}", line);
				throw new IllegalArgumentException();
			}
			String user=fields[0];
			String key= fields[1];
			String dn=fields[2];
			return new AttributesHolder(user, key, dn);
		}

		public static AttributesHolder fromAttributes(String userName, String dnAssign, Map<String,Object> attributes) {
			AttributesHolder ah = new AttributesHolder(userName);
			if(dnAssign!=null && attributes!=null && attributes.size()>0) {
				ah.dn = RESTUtils.evaluateToString(dnAssign, attributes);
			}
			return ah;
		}
	}

	public static class AttributeHolders {

		private final List<AttributesHolder> coll = new ArrayList<>();

		private long lastUpdated;

		public boolean wantUpdate(){
			return lastUpdated+1000*updateInterval<System.currentTimeMillis();
		}

		public void refresh(){
			lastUpdated = System.currentTimeMillis();
		}

		public void invalidate(){
			lastUpdated = 0;
		}

		public synchronized void removeFileEntries(){
			filterEntries(true);
		}

		private void filterEntries(boolean fromFile){
			Iterator<AttributesHolder>attrs = coll.iterator();
			while(attrs.hasNext()){
				AttributesHolder ah = attrs.next();
				if(ah.fromFile == fromFile)attrs.remove();
			}
		}

		public synchronized void add(AttributesHolder ah) {
			coll.add(ah);
		}

		/**
		 * get a (read-only) view of the current list off keys
		 */
		public synchronized List<AttributesHolder> get(){
			return new ArrayList<>(coll);
		}

		public synchronized void mergeKeysFromServer(Collection<AttributesHolder> newAttrs){
			filterEntries(false);
			coll.addAll(newAttrs);
		}
	}

	public static boolean isValidKey(String text){
		try{
			SSHUtils.readPubkey(text);
			return true;
		}
		catch(Exception ex){
			return false;
		}
	}

	public static interface UserInfoSource {

		/**
		 * retrieve public keys and other attributes
		 *
		 * @param userName
		 * @param identityAssign - optional mapping expression for assigning the user identity (DN) 
		 */
		public AttributesHolder getAttributes(String userName, String identityAssign);

	}
}
