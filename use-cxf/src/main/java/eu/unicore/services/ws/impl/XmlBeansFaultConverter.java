package eu.unicore.services.ws.impl;

import java.util.concurrent.TimeoutException;

import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.exceptions.TerminationTimeChangeRejectedException;
import eu.unicore.services.exceptions.UnableToSetTerminationTimeException;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;
import eu.unicore.services.ws.exceptions.TerminationTimeChangeRejectedFault;
import eu.unicore.services.ws.exceptions.UnableToSetTerminationTimeFault;
import eu.unicore.util.Log;

/**
 * convert the wsrf lifecycle related exceptions to the correct SOAP Fault types
 */
public class XmlBeansFaultConverter {

	private static XmlBeansFaultConverter inst;
	
	public Exception convert(Exception e) {
		if(e instanceof ResourceUnknownException){
			String msg=Log.getDetailMessage(e);
			return ResourceUnknownFault.createFault("Resource unknown: "+msg);
		}
		if(e instanceof ResourceUnavailableException){
			String msg=Log.getDetailMessage(e);
			return ResourceUnavailableFault.createFault("Resource unavailable: "+msg);	
		}
		if(e instanceof TerminationTimeChangeRejectedException){
			String msg=Log.getDetailMessage(e);
			return TerminationTimeChangeRejectedFault.createFault("TT change rejected: "+msg);	
		}
		if(e instanceof UnableToSetTerminationTimeException){
			String msg=Log.getDetailMessage(e);
			return UnableToSetTerminationTimeFault.createFault("Could not set TT: "+msg);	
		}
		if(e instanceof TimeoutException){
			String msg=Log.getDetailMessage(e);
			return ResourceUnavailableFault.createFault("Resource unavailable: "+msg);
		}
		
		return BaseFault.createFault(e.getMessage(),e);
	}

	
	public static synchronized XmlBeansFaultConverter getInstance(){
		if(inst==null){
			inst=new XmlBeansFaultConverter();
		}
		return inst;
	}
}
