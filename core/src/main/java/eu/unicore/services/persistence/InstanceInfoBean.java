package eu.unicore.services.persistence;

import java.io.Serializable;
import java.util.Calendar;

import eu.unicore.persist.annotations.Column;
import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.annotations.Table;

/**
 * for persisting per-instance information relevant to the container, such as 
 * termination time information
 */
@Table(name="WSRFInstanceInformation")
public class InstanceInfoBean implements Serializable {
	
	private static final long serialVersionUID=1L;
	
	@ID
	public String uniqueID;
	
	@Column(name="service")
	public String serviceName;
	
	@Column(name="terminates")
	public String terminates;
	
	@Column(name="millis")
	private long date;
	
	private long subscriberCount=0;
	
	public InstanceInfoBean(String uniqueID, String serviceName, Calendar terminationTime){
		this.uniqueID=uniqueID;
		this.serviceName=serviceName;
		if(terminationTime!=null){
			this.terminates=terminationTime.getTime().toString();
			this.date=terminationTime.getTimeInMillis();
		}
		else{
			this.terminates="infinite";
			this.date=0;
		}
	}
	
	/**
	 * get the termination time
	 * @return Calendar or <code>null</code> if TT is infinite
	 */
	public Calendar getTerminationTime(){
		Calendar tt=null;
		if(date>0){
			tt=Calendar.getInstance();
			tt.setTimeInMillis(date);
		}
		return tt;
	}
	
	//needed by persistence lib
	
	public String getUniqueID(){
		return uniqueID;
	}
	
	public String getServiceName(){
		return serviceName;
	}
	
	public String getTerminates(){
		return terminates;
	}
	
	public String getDate(){
		return String.valueOf(date);
	}
	
	public String getSubscriberCount(){
		return String.valueOf(subscriberCount);
	}
	
	public void incrementSubscriberCount(){
		subscriberCount++;
	}
	
	public void decrementSubscriberCount(){
		subscriberCount--;
		if(subscriberCount<0)subscriberCount=0;
	}
	
	public static Calendar getCalendar(String dateMillis){
		Calendar tt=null;
		long millis=Long.valueOf(dateMillis);
		if(millis>0){
			tt=Calendar.getInstance();
			tt.setTimeInMillis(millis);
		}
		return tt;
	}
	
	
}
