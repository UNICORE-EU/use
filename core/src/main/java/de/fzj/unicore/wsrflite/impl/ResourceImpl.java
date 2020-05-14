/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.wsrflite.impl;

import java.util.Calendar;

import org.apache.log4j.Logger;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.ExtendedResourceStatus;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Model;
import de.fzj.unicore.wsrflite.exceptions.TerminationTimeChangeRejectedException;
import de.fzj.unicore.wsrflite.exceptions.UnableToSetTerminationTimeException;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import eu.unicore.util.Log;

/**
 * Base implementation of the USE lifecycle interfaces
 * 
 * Security features are handled in the {@link SecuredResourceImpl} base class.
 * 
 * Handles termination time. The initial termination time is defined when the resource is
 * created.
 * 
 * Extended state: this class supports the {@link ExtendedResourceStatus} interface which allows
 * to have more complex lifecycle, especially supporting an asynchronous initialisation phase
 * 
 * @author schuller
 */
public abstract class ResourceImpl extends SecuredResourceImpl implements ExtendedResourceStatus {

	private static final Logger logger=Log.getLogger(Log.WSRFLITE, ResourceImpl.class);

	protected Home home;

	protected Kernel kernel;

	private boolean isDestroyed=false;

	public String getServiceName(){
		return home!=null? home.getServiceName() : null;
	}

	public void setTerminationTime(Calendar newTT) 
			throws TerminationTimeChangeRejectedException,UnableToSetTerminationTimeException {
		if(home!=null)home.setTerminationTime(getModel().getUniqueID(),newTT);
	}

	/**
	 * perform resource cleanup
	 * If you overwrite this method to perform specific clean-up, remember to invoke
	 * super.destroy() at the end of your code
	 */
	public void destroy() {
		try{
			((DefaultHome)home).instanceDestroyed(getOwner());
		}catch(Exception ex){
			Log.logException("Error decreasing number of service instances.", ex, logger);
		}
		isDestroyed=true;
	}

	public final boolean isDestroyed(){
		return isDestroyed;
	}

	public void setHome(Home home){
		this.home=home;
	}

	public Home getHome(){
		return home;
	}

	@Override
	public void setKernel(Kernel kernel) {
		this.kernel=kernel;
	}

	@Override
	public Kernel getKernel() {
		return kernel;
	}
	
	@Override
	public void initialise(InitParameters initParams) throws Exception {
		if(getModel()==null){
			setModel(new BaseModel());
		}
		
		String uniqueID = initParams.uniqueID;
		
		getModel().setUniqueID(uniqueID);
		getModel().setParentServiceName(initParams.parentServiceName);
		getModel().setParentUID(initParams.parentUUID);
		
		//security stuff
		super.initialise(initParams);

		Calendar tt = null;
		if(TerminationMode.DEFAULT == initParams.terminationMode){
			tt=Calendar.getInstance();
			tt.add(Calendar.SECOND,getDefaultLifetime());
		}
		else if(TerminationMode.NEVER != initParams.terminationMode){
			 tt = initParams.getTerminationTime();
			 if(tt==null)throw new IllegalArgumentException("No termination time given!");
		}
		if(TerminationMode.NEVER != initParams.terminationMode){
			setTerminationTime(tt);
		}	
		setResourceStatus(initParams.resourceState);
		
		if(logger.isDebugEnabled()){
			logger.debug("Initialised <"+getServiceName()+">"+uniqueID+ (tt!=null ? ", TT = "+tt.getTime(): ""));
		}

	}

	/**
	 * helper method that returns the default lifetime of a 
	 * ws-resource of the given service in seconds
	 * 
	 * @see ContainerProperties#DEFAULT_LIFETIME
	 * @return lifetime
	 */
	protected int getDefaultLifetime(){
		return getKernel().getContainerProperties().getSubkeyIntValue(
				ContainerProperties.DEFAULT_LIFETIME, getServiceName());
	}
	
	@Override
	public BaseModel getModel(){
		return (BaseModel)model;
	}
	
	@Override
	public Model passivate(){
		return getModel();
	}

	@Override
	public void activate(){
	}

	@Override
	public void processMessages(PullPoint messageIterator){
	}

	@Override
	public void postRestart()throws Exception{
		//NOP
	}

	@Override
	public boolean isReady(){
		return ResourceStatus.READY==getModel().getResourceStatus();
	}
	
	@Override
	public String getStatusMessage() {
		return getModel().getResourceStatusDetails();
	}

	@Override
	public ResourceStatus getResourceStatus() {
		return getModel().getResourceStatus();
	}

	@Override
	public void setStatusMessage(String message) {
		getModel().setResourceStatusDetails(message);
	}

	@Override
	public void setResourceStatus(ResourceStatus status) {
		getModel().setResourceStatus(status);
	}

}
