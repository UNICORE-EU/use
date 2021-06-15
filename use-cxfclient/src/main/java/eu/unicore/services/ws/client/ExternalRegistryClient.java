package eu.unicore.services.ws.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.sg2.AddDocument;
import org.oasisOpen.docs.wsrf.sg2.AddResponseDocument;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.util.Log;

/**
 * A client for querying and adding to a set of external registries.
 * 
 * By default, the lists of services do not contain duplicates.
 * You can change this behaviour using the setFilterDuplicates() method.
 *  
 * @see MultiWSRFClient
 * @author schuller
 */
public class ExternalRegistryClient extends MultiWSRFClient<RegistryClient> implements IRegistryQuery {
	
	protected static final long readd_offset = 60000; 
	
	private boolean filterDuplicates=true;
	
	public ExternalRegistryClient(){
		super();
	}
	
	/**
	 * calls "add" on all configured external registries<br>
	 * This method will return the shortest termination time returned by 
	 * any of the external registries, or a default of 5 minutes in case 
	 * no external registry can be contacted.  
	 * 
	 * @param in - an AddDocument
	 * @return Calendar
	 */
	public Calendar addRegistryEntry(AddDocument in){
		long current=System.currentTimeMillis();
		Calendar responseTT=Calendar.getInstance();
		//set a default termination time, in case something goes wrong when talking to 
		//the external registries
		responseTT.add(Calendar.SECOND, 300);
		long selected=responseTT.getTimeInMillis();
		boolean haveExternalTT=false;
		for(RegistryClient reg: clients){
			String address = reg.getEPR().getAddress().getStringValue();
			try{
				logger.debug("Calling external registry: {}", address);
				Calendar c=reg.getCurrentTime();
				long timeDifference = current - c.getTimeInMillis();
				AddResponseDocument res=reg.addRegistryEntry(in);
				Calendar extTT=res.getAddResponse().getTerminationTime();
				logger.debug("External registry {} requests lifetime: {}", address, extTT.getTime());
				//select the "earliest" termination time reported back from the external registries
				//and subtract 60 "grace seconds"
				long tt = extTT.getTimeInMillis()+timeDifference-readd_offset;
				if(tt<selected || !haveExternalTT){
					selected=tt;
					haveExternalTT=true;
				}
			}catch(Exception e){
				logger.debug(Log.createFaultMessage("Can't talk to registry "+address ,e));
			}
		}
		responseTT.setTimeInMillis(selected);
		return responseTT;
	}
	
	public Calendar addRegistryEntry(String endpoint, Map<String,String> content){
		return addRegistryEntry(RegistryClient.makeAddRequest(endpoint, content));
	}

	public List<EndpointReferenceType> listAccessibleServices(QName porttype) throws Exception {
		List<EndpointReferenceType>result=new ArrayList<EndpointReferenceType>();
		for(RegistryClient c: clients){
			if(!c.checkConnection())continue;
			List<EndpointReferenceType>res=c.listAccessibleServices(porttype);
			if(filterDuplicates){
				addIfNotExist(result, res);
			}else{
				result.addAll(res);
			}
		}
		return result;
	}

	/**
	 * List all the entries in all the registries. No duplicate filtering
	 * is applied
	 */
	public List<EntryType> listEntries() {
		List<EntryType>result=new ArrayList<EntryType>();
		for(RegistryClient c: clients){
			try{
				result.addAll(c.listEntries());
			}catch(Exception ex){
				Log.logException("Registry at "+c.getEPR().getAddress().getStringValue()+ " is not available.", ex, logger);
			}
		}
		return result;
	}

	public List<EndpointReferenceType> listServices(QName porttype, ServiceListFilter acceptFilter) throws Exception {
		List<EndpointReferenceType>result=new ArrayList<EndpointReferenceType>();
		for(RegistryClient c: clients){
			if(!c.checkConnection())continue;
			List<EndpointReferenceType>res=c.listServices(porttype,acceptFilter);
			if(filterDuplicates){
				addIfNotExist(result, res);
			}else{
				result.addAll(res);
			}
		}
		return result;
	}

	public List<EndpointReferenceType> listServices(QName porttype) throws Exception {
		List<EndpointReferenceType>result=new ArrayList<EndpointReferenceType>();
		for(RegistryClient c: clients){
			if(!c.checkConnection())continue;
			List<EndpointReferenceType>res=c.listServices(porttype);
			if(filterDuplicates){
				addIfNotExist(result, res);
			}else{
				result.addAll(res);
			}
		}
		return result;
	}
	

	/**
	 * check the connection to the services (using the default timeout of 2 seconds). 
	 * If the service does not reply within the timeout, returns <code>false</code>
	 */
	public boolean checkConnection(){
		return checkConnection(2000);
	}

	private String connectionStatus=null;
	
	/**
	 * check the connection to the services. If no service 
	 * replies within the given timeout, returns <code>false</code>
	 * 
	 * @param timeout - connection timeout in milliseconds
	 */
	public boolean checkConnection(int timeout){
		final StringBuffer status=new StringBuffer();
		boolean result=false;
		for(final RegistryClient c: clients){
			Callable<Boolean>task=new Callable<Boolean>(){
				public Boolean call()throws Exception{
					c.getCurrentTime();
					return Boolean.TRUE;
				}
			};
			
			Boolean res=compute(task, timeout);
			boolean currentOK=res!=null?res.booleanValue():false;
			if(!currentOK){
				status.append("[NOT AVAILABLE: ").append(c.getEPR().getAddress().getStringValue());
				status.append("] ");
			}
			result=result || currentOK;
			
		}
		if(result)connectionStatus="OK";
		else connectionStatus=status.toString();
		
		return result;
	}
	
	public String getConnectionStatus(){
		checkConnection();
		return connectionStatus;		
	}
	
	private void addIfNotExist(List<EndpointReferenceType>target, List<EndpointReferenceType>source){
		Set<String>addresses=new HashSet<String>();
		for(EndpointReferenceType epr: target){
			addresses.add(epr.getAddress().getStringValue());
		}
		for(EndpointReferenceType e: source){
			String address=e.getAddress().getStringValue();
			if(!addresses.contains(address)){
				addresses.add(address);
				target.add(e);
			}
		}
	}

	public boolean isFilterDuplicates() {
		return filterDuplicates;
	}

	public void setFilterDuplicates(boolean filterDuplicates) {
		this.filterDuplicates = filterDuplicates;
	}
	
	private Boolean compute(Callable<Boolean>task, int timeout){
		try{
			Future<Boolean>f=Resources.getExecutorService().submit(task);
			return f.get(timeout, TimeUnit.MILLISECONDS);
		}catch(Exception ex){
			return Boolean.FALSE;
		}
	}
	
}
