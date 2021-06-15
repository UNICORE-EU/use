package eu.unicore.services.rest.testservice;

import eu.unicore.security.OperationType;
import eu.unicore.services.InitParameters;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.ACLEntry.MatchType;

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
