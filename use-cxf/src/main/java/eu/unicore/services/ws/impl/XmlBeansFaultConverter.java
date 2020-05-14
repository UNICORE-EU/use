package eu.unicore.services.ws.impl;

import java.util.concurrent.TimeoutException;

import de.fzj.unicore.wsrflite.exceptions.ResourceUnavailableException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.exceptions.TerminationTimeChangeRejectedException;
import de.fzj.unicore.wsrflite.exceptions.UnableToSetTerminationTimeException;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
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
