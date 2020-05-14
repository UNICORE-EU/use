package eu.unicore.services.rest.testservice;

import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import de.fzj.unicore.wsrflite.security.ACLEntry;
import de.fzj.unicore.wsrflite.security.ACLEntry.MatchType;
import eu.unicore.security.OperationType;

public class CounterResource extends ResourceImpl {

	public static volatile int processedMessages = 0;
	
	@Override
	public CounterModel getModel() {
		return (CounterModel)super.getModel();
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		if(getModel()==null){
			setModel(new CounterModel());
		}
		// add acl entries
		ACLEntry e = new ACLEntry(OperationType.modify, "CN=Test", MatchType.DN);
		ACLEntry e1 = new ACLEntry(OperationType.read, "testers", MatchType.GROUP);
		getModel().getAcl().add(e);
		getModel().getAcl().add(e1);
		super.initialise(initParams);
	}

	@Override
	public void processMessages(PullPoint messageIterator) {
		while(messageIterator.hasNext()){
			messageIterator.next();
			processedMessages++;
		}
	}

	
}
