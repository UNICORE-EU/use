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
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;

/**
 * use the Persistence framework for storing resource instances
 * 
 * @author schuller
 */
public class Persistence implements Store {

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,Persistence.class);

	private Persist<ResourceBean> p;

	private PersistenceProperties persistenceProperties;

	// shared storage for the termination times on this server
	private static Persist<InstanceInfoBean> terminationTime;

	private String serviceName;

	private Kernel kernel;

	@Override
	public void init(Kernel kernel, String serviceName) throws Exception {
		this.kernel = kernel;
		this.serviceName = serviceName;
		persistenceProperties = kernel.getPersistenceProperties();
		initTerminationTimeStore();
		p = PersistenceFactory.get(persistenceProperties).getPersist(ResourceBean.class, serviceName);
	}

	// testing
	Persist<ResourceBean> getBackEnd() {
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
	private String[] makeTagsUnique(String... tags) {
		String[] res = new String[tags.length];
		for(int i = 0; i<tags.length; i++) {
			res[i] = ","+tags[i]+",";
		}
		return res;
	}

	@Override
	public void persist(Resource inst)throws Exception{
		ResourceBean bean= new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						inst.getModel());
		p.write(bean);
	}

	@Override
	public Resource read(String uniqueID)throws Exception{
		ResourceBean bean = p.read(uniqueID);
		if(bean!=null){							
			return createResource(bean);
		}
		return null;
	}

	@Override
	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit) throws Exception{
		ResourceBean bean = p.getForUpdate(uniqueID,time,timeUnit);
		if(bean!=null){							
			 return createResource(bean);
		}
		return null;
	}

	private Resource createResource(ResourceBean bean) throws Exception {
		// backwards compatibility hacks...
		if(bean.className.startsWith("de.fzj.unicore.uas.")) {
		   bean.className = bean.className.replace("de.fzj.unicore.uas.", "eu.unicore.uas.");
                }
                else if(bean.className.equals("eu.unicore.services.registry.LocalRegistryImpl")) {
			bean.className = "eu.unicore.services.rest.registry.LocalRegistryImpl";
		}
		Class<?> clazz = Class.forName(bean.className);
		Resource inst = (Resource)clazz.getConstructor().newInstance();
		inst.setKernel(kernel);
		inst.setModel(bean.getState());
		return inst;
	}

	@Override
	public void remove(String uniqueID) throws Exception {
		p.remove(uniqueID);
		terminationTime.remove(uniqueID);
	}

	@Override
	public void unlock(Resource inst) throws Exception {
		ResourceBean bean = new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						null);

		p.unlock(bean);
	}

	private volatile boolean isShutdown=false;

	@Override
	public void shutdown() {
		if(isShutdown)return;
		try{
			isShutdown = true;
			p.shutdown();
		}catch(Exception pe){
			logger.error("Error shutting down persistence for <{}>", serviceName, pe);
		}
	}

	@Override
	public Map<String, Calendar> getTerminationTimes() throws Exception {
		Map<String,Calendar>tt = new ConcurrentHashMap<>();
		Map<String,String>tts = terminationTime.getColumnValues("millis");
		for(Map.Entry<String,String> e: tts.entrySet()){
			tt.put(e.getKey(), InstanceInfoBean.getCalendar(e.getValue()));
		}
		return tt;
	}

	@Override
	public void setTerminationTime(String uniqueID, Calendar c)throws Exception{
		InstanceInfoBean t=terminationTime.getForUpdate(uniqueID,100,TimeUnit.MILLISECONDS);
		try{
			t=new InstanceInfoBean(uniqueID,serviceName,c);
		}finally{
			terminationTime.write(t);
		}
	}

	private synchronized void initTerminationTimeStore() throws Exception {
		if(terminationTime==null) {
			terminationTime = PersistenceFactory.get(persistenceProperties).getPersist(InstanceInfoBean.class);
		}
	}

}
