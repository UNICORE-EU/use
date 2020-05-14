package eu.unicore.services.ws.exampleservice;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;

@WebService
public interface IExampleService {
	@WebMethod()
	public GetResourcePropertyResponseDocument 
		GetResourceProperty(GetResourcePropertyDocument in) 
		throws BaseFault;
	
	@WebMethod(action="foo")
	SetTerminationTimeResponseDocument throwBaseFault(SetTerminationTimeDocument req)
	throws ResourceUnavailableFault,BaseFault;
	
}
