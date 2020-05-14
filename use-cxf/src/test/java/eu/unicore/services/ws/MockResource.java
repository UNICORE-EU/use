package eu.unicore.services.ws;

import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.impl.BaseModel;
import de.fzj.unicore.wsrflite.security.ACLEntry;
import de.fzj.unicore.wsrflite.security.ACLEntry.MatchType;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.InvalidResourcePropertyQNameFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import eu.unicore.security.OperationType;
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