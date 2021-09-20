package eu.unicore.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.persistence.PersistenceSettings;
import eu.unicore.services.persistence.Store;

public class MockStore implements Store {

	private Map<String,Calendar>tt=new HashMap<String, Calendar>();
	
	public MockStore() {
		super();
	}

	public void init(Kernel k, String serviceName) {
	}

	public void shutdown() {
	}

	public void persist(Resource inst) {
	}

	public void lock(Resource inst, long timeout, TimeUnit units){
	}

	public void unlock(Resource inst) {
	}
	
	public List<String>getUniqueIDs() {
		return new ArrayList<>();
	}

	public List<String>getTaggedResources(String... tags) {
		return new ArrayList<String>();
	}
	
	public Resource read(String uniqueID) {
		return null;
	}


	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit) {
		return null;
	}
	
	public void remove(String uniqueID) {
	}

	public int size() {
		return 0;
	}

	public PersistenceSettings getPersistenceSettings() {
		return null;
	}

	public void setPersistenceSettings(PersistenceSettings ps) {
	}

	public Map<String, Calendar> getTerminationTimes() {
		return tt;
	}

	public void setTerminationTime(String uniqueID, Calendar c) throws Exception {
		if(c==null)tt.remove(uniqueID);
		else tt.put(uniqueID, c);
	}

	public void purgePersistentData() {
		tt.clear();
	}

	public void putData(String key, Object value) {
	}

	public Object getData(String key) {
		return null;
	}
	
	public void flush(){}
	
}
