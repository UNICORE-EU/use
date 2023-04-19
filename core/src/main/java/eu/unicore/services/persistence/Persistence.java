package eu.unicore.services.persistence;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.PersistImpl;
import eu.unicore.persist.impl.PersistenceDescriptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.UnableToSetTerminationTimeException;
import eu.unicore.util.Log;

/**
 * use the Persistence framework for storing resource instances
 * 
 * @author schuller
 */
public class Persistence extends AbstractStore{

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,Persistence.class);

	private Persist<ResourceBean>p;

	private PersistenceProperties persistenceProperties;

	//database for storing all the termination times on this server
	private static Persist<InstanceInfoBean>terminationTime;

	@Override
	public void init(Kernel kernel, String serviceName) {
		super.init(kernel, serviceName);
		persistenceProperties = kernel.getPersistenceProperties();
		try{
			PersistenceDescriptor pd=PersistenceDescriptor.get(ResourceBean.class);
			pd.setTableName(serviceName);
			p=PersistenceFactory.get(persistenceProperties).getPersist(ResourceBean.class,pd);
			initTerminationTimeStore();
		} catch (Exception e) {
			Log.logException("Error initialising database for <"+serviceName+">",e,logger);
			//make sure this Persistence impl will not work at all
			p=null; 
			terminationTime=null;
			throw new RuntimeException(e);
		}
	}

	// testing
	Persist<ResourceBean>getBackEnd(){
		return p;
	}

	@Override
	public List<String> getUniqueIDs() throws Exception {
		return p.getIDs();
	}

	@Override
	public List<String> getTaggedResources(String... tags) throws Exception {
		return p.findIDs("tags", makeTagsUnique(tags));
	}
	
	// protects against matching substrings of tags
	protected String[]makeTagsUnique(String... tags){
		String[] res = new String[tags.length];
		for(int i = 0; i<tags.length; i++) {
			res[i] = ","+tags[i]+",";
		}
		return res;
	}
	
	@Override
	protected void _persist(ResourceBean data)throws Exception {
		p.write(data);
	}

	@Override
	protected ResourceBean _read(String uniqueID) throws Exception{
		return p.read(uniqueID);
	}

	@Override
	protected ResourceBean _getForUpdate(String uniqueID,long time, TimeUnit timeUnit) 
			throws Exception {
		return p.getForUpdate(uniqueID,time,timeUnit);
	}

	@Override
	protected void _remove(String uniqueID) {
		try{
			p.remove(uniqueID);
		}catch(Exception e){
			Log.logException("Error",e,logger);
		}
		try{
			terminationTime.remove(uniqueID);
		}catch(Exception e){
			Log.logException("Error removing TT entry",e,logger);
		}
	}

	public void purgePersistentData() {
		p.purge();
	}

	protected void _lock(ResourceBean dao,long timeout, TimeUnit timeUnit)throws Exception {
		p.lock(dao.getUniqueID(), timeout, timeUnit);
	}

	protected void _unlock(ResourceBean dao)throws Exception{
		p.unlock(dao);
	}

	public void removeAll()throws Exception{
		p.removeAll();
	}

	private volatile boolean isShutdown=false;

	public void shutdown() {
		if(isShutdown)return;
		try{
			isShutdown=true;
			p.shutdown();
		}catch(Exception pe){
			logger.error("Error shutting down persistence for <"+getServiceName()+">",pe);
		}
	}

	public Map<String, Calendar> getTerminationTimes(){
		Map<String,Calendar>tt=new ConcurrentHashMap<>();
		try{
			Map<String,String>tts=terminationTime.getColumnValues("millis");
			for(Map.Entry<String,String> e: tts.entrySet()){
			    tt.put(e.getKey(), InstanceInfoBean.getCalendar(e.getValue()));
			}
		}catch(Exception pe){
			Log.logException("Error getting termination times from data base.", pe,logger);
		}	
		return tt;
	}

	public void setTerminationTime(String uniqueID, Calendar c)throws UnableToSetTerminationTimeException{
		try{
			InstanceInfoBean t=terminationTime.getForUpdate(uniqueID,100,TimeUnit.MILLISECONDS);
			try{
				t=new InstanceInfoBean(uniqueID,serviceName,c);
			}finally{
				terminationTime.write(t);
			}
		}catch(Exception ex){
			throw new UnableToSetTerminationTimeException("Cannot set termination time.",ex);
		}
	}

	private synchronized void initTerminationTimeStore() throws Exception {
		if(terminationTime!=null)return;
		PersistenceDescriptor pd=PersistenceDescriptor.get(InstanceInfoBean.class);
		pd.setTableName("TerminationTimes");
		terminationTime=PersistenceFactory.get(persistenceProperties).getPersist(InstanceInfoBean.class, pd);
	}

	@Override
	public long getCacheHits(){
		return ((PersistImpl<ResourceBean>)p).getCacheHits();
	}

}
