package eu.unicore.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;
import eu.unicore.services.security.ACLEntry;

public class InitParameters {

	public enum TerminationMode {
		NEVER,   // the resource never expires 
		DEFAULT; // the configured default termination time is used
	};
	
	public final String uniqueID;
	
	public final TerminationMode terminationMode;
	
	public final Calendar terminationTime;
	
	public Calendar getTerminationTime(){
		return terminationTime;
	}

	/**
	 * init with auto-generated uuid and the default termination time
	 */
	public InitParameters(){
		this(null, TerminationMode.DEFAULT);
	}
	
	/**
	 * for a resource with the default termination time
	 * @param uuid
	 */
	public InitParameters(String uuid){
		this(uuid, TerminationMode.DEFAULT);
	}
	
	/**
	 * 
	 * @param uuid - unique ID, if null, it will be auto-generated
	 * @param terminationMode - NEVER expire or use DEFAULT lifetime
	 */
	public InitParameters(String uuid, TerminationMode terminationMode){
		this(uuid, terminationMode, null);
		if(terminationMode==null)throw new IllegalArgumentException("Termination mode cannot be null");
	}
	
	/**
	 * @param uuid - unique ID, if null, it will be auto-generated
	 * @param terminationTime
	 */
	public InitParameters(String uuid, Calendar terminationTime){
		this(uuid, null, terminationTime);
		if(terminationTime==null)throw new IllegalArgumentException("TT cannot be null");
	}
	
	/**
	 * internal constructor
	 * @param uuid
	 * @param terminationMode
	 * @param terminationTime
	 */
	protected InitParameters(String uuid, TerminationMode terminationMode, Calendar terminationTime){
		uniqueID = uuid!=null? uuid : UUID.randomUUID().toString();
		this.terminationMode = terminationMode;
		this.terminationTime = terminationTime;
	}
	
	public String ownerDN = null;
	
	public List<ACLEntry> acl = new ArrayList<>();
	
	public ResourceStatus resourceState = ResourceStatus.READY;
	
	public boolean publishToRegistry = false;
	
	public String resourceClassName;
	
	public String parentUUID;
	
	public String parentServiceName;
	
	public final Map<String,String>extraParameters = new HashMap<>();

}
