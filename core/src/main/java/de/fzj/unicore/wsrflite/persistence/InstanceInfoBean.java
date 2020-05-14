/*********************************************************************************
 * Copyright (c) 2006-2010 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package de.fzj.unicore.wsrflite.persistence;

import java.io.Serializable;
import java.util.Calendar;

import de.fzj.unicore.persist.annotations.Column;
import de.fzj.unicore.persist.annotations.ID;
import de.fzj.unicore.persist.annotations.Table;

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
