package eu.unicore.services.ws.exampleservice;

import java.util.Calendar;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;

public class ExampleService implements IExampleService {
	
	
	public GetResourcePropertyResponseDocument GetResourceProperty(GetResourcePropertyDocument in) throws BaseFault {
		GetResourcePropertyResponseDocument res=
			GetResourcePropertyResponseDocument.Factory.newInstance();
		XmlObject o=XmlObject.Factory.newInstance();
		XmlCursor c=o.newCursor();
		c.toFirstContentToken();
		c.beginElement(new QName("World!","Hello"));
		
		res.addNewGetResourcePropertyResponse().set(o);
		return res;
	}
	
	public SetTerminationTimeResponseDocument throwBaseFault(SetTerminationTimeDocument req) throws ResourceUnavailableFault, BaseFault {
		if(req.getSetTerminationTime().isNil()){
			SetTerminationTimeResponseDocument res=SetTerminationTimeResponseDocument.Factory.newInstance();
			res.addNewSetTerminationTimeResponse().setNewTerminationTime(Calendar.getInstance());
			return res;
		}
		else{
			throw BaseFault.createFault("");
		}
	}

}
