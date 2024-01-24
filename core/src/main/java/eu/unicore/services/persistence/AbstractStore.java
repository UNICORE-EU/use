package eu.unicore.services.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;

/**
 * Base functionality shared by all persistence implementations
 * provides optional load-once semantics
 * 
 * @see eu.unicore.services.persistence.LoadSemantics
 * 
 * @author schuller
 * @author j.daivandy@fz-juelich.de
 */
public abstract class AbstractStore implements Store {

	private static final Logger logger=Log.getLogger(Log.PERSISTENCE,AbstractStore.class);

	protected String serviceName;

	protected PersistenceSettings settings=PersistenceSettings.getDefaultSettings();

	protected ConcurrentMap<String,Resource> instances;

	protected Kernel kernel;

	public synchronized void init(Kernel kernel, String serviceName) {
		this.kernel = kernel;
		this.serviceName = serviceName;
		instances = new ConcurrentHashMap<>();
	}

	public void persist(Resource inst)throws Exception{
		settings=kernel.getPersistenceManager().getPersistenceSettings(inst.getClass());
		if(settings.isLoadOnce())instances.put(inst.getUniqueID(),inst);
		ResourceBean bean=
				new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						inst.getModel());

		_persist(bean);
	}

	public void lock(Resource inst, long timeout, TimeUnit units) throws Exception{
		ResourceBean bean=
				new ResourceBean(
						inst.getUniqueID(),
						inst.getServiceName(),
						inst.getClass().getName(),
						null);

		_lock(bean,timeout,units);
	}

	public void unlock(Resource inst) {
		try {
			ResourceBean bean=
					new ResourceBean(
							inst.getUniqueID(),
							inst.getServiceName(),
							inst.getClass().getName(),
							null);

			_unlock(bean);
		} catch (Exception e) {
			String msg=inst!=null?inst.getServiceName()+"["+inst.getUniqueID()+"]":"[null]";
			Log.logException("Error unlocking "+msg,e,logger);
		}
	}

	protected abstract void _lock(ResourceBean dao,long timeout, TimeUnit timeUnit) throws Exception;
	
	protected abstract void _unlock(ResourceBean bean)throws Exception;

	protected abstract void _persist(ResourceBean bean)throws Exception;

	public Resource read(String uniqueID)throws Exception{
		Resource inst=null;
		if(settings.isLoadOnce()){
			inst=instances.get(uniqueID);
			if(inst!=null){
				inst.setKernel(kernel);
				return inst;
			}
		}
		ResourceBean bean = _read(uniqueID);
		if(bean!=null){							
			inst = createResource(bean);
		}
		return inst;
	}
	
	protected Resource createResource(ResourceBean bean) throws Exception {
		Resource inst = null;
		Class<?> clazz = Class.forName(bean.className);
		inst=(Resource)clazz.getConstructor().newInstance();
		inst.setKernel(kernel);
		inst.setHome(kernel.getHome(serviceName));
		inst.setModel(bean.getState());
		if(settings.isLoadOnce()){
			instances.put(bean.uniqueID,inst);
		}
		return inst;
	}
	
	protected abstract ResourceBean _read(String uniqueID) throws Exception;

	public Resource getForUpdate(String uniqueID, long time, TimeUnit timeUnit) throws Exception{
		Resource inst=null;
		if(settings.isLoadOnce()){
			inst=instances.get(uniqueID);
		}
		else {
			ResourceBean bean = _getForUpdate(uniqueID, time, timeUnit);
			if(bean!=null){							
				inst=createResource(bean);
			}
		}
		return inst;
	}

	protected abstract ResourceBean _getForUpdate(String uniqueID, long time, TimeUnit timeUnit) throws Exception;

	public void remove(String uniqueID)throws Exception{
		_remove(uniqueID);
		if(settings.isLoadOnce())instances.remove(uniqueID);
	}

	protected abstract void _remove(String uniqueID) throws Exception;

	public int size() throws Exception{
		return getUniqueIDs().size();
	}

	public long getCacheHits(){
		return -1;
	}

	public void setPersistenceSettings(PersistenceSettings ps) {
		this.settings=ps;		
	}

	public PersistenceSettings getPersistenceSettings () {
		return settings;
	}

	public String getServiceName(){
		return serviceName;
	}

	/**
	 * get the current statistics for this AbstractStore
	 */
	public Map<String,String>getStatistics(){
		return null;
	}

}
