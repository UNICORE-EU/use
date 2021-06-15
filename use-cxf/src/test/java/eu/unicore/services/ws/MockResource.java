package eu.unicore.services.ws;

import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import eu.unicore.security.OperationType;
import eu.unicore.services.InitParameters;
import eu.unicore.services.impl.BaseModel;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.ACLEntry.MatchType;
import eu.unicore.services.ws.exceptions.InvalidResourcePropertyQNameFault;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;
import eu.unicore.services.ws.exceptions.TerminationTimeChangeRejectedFault;
import eu.unicore.services.ws.exceptions.UnableToSetTerminationTimeFault;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.ConcurrentAccess;

/**
 * Unit testing only!
 *
 * @author schuller
 */
public class MockResource extends WSResourceImpl{
	public QName getResourcePropertyDocumentQName() {
		return null;
	}
	
	public static boolean slow=false;
	
	public static int sleep=5000;

	@Override
	@ConcurrentAccess(allow=false)
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in)
			throws UnableToSetTerminationTimeFault,
			TerminationTimeChangeRejectedFault, ResourceUnknownFault,
			ResourceUnavailableFault {
		if(slow){
			System.out.println("Entering slow serial method");
			try{
				Thread.sleep(sleep);
			}catch(InterruptedException ie){}
		}
		return super.SetTerminationTime(in);
	}

	@Override
	@ConcurrentAccess(allow=true)
	public GetResourcePropertyResponseDocument GetResourceProperty(
			GetResourcePropertyDocument in) throws BaseFault,
			ResourceUnknownFault, ResourceUnavailableFault,
			InvalidResourcePropertyQNameFault {
		if(slow){
			System.out.println("Entering slow concurrent method");
			try{
				Thread.sleep(sleep);
			}catch(InterruptedException ie){}
		}
		return super.GetResourceProperty(in);
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		super.initialise(initParams);
		BaseModel m = getModel();

		ACLEntry e = new ACLEntry(OperationType.read, "CN=Test", MatchType.DN);
		m.getAcl().add(e);
	}
	
}