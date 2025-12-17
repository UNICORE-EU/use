package eu.unicore.services.rest.testservice;

import eu.unicore.security.OperationType;
import eu.unicore.services.ExtendedResourceStatus;
import eu.unicore.services.InitParameters;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.ACLEntry.MatchType;

public class CounterResource extends ResourceImpl implements ExtendedResourceStatus {

	public static volatile int processedMessages = 0;

	@Override
	public CounterModel getModel() {
		return (CounterModel)super.getModel();
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		CounterInitParams cip = (CounterInitParams)initParams;
		CounterModel cm = getModel();
		if(cm==null){
			cm = new CounterModel();
			if(cip.initDelay==666) {
				// special value to trigger init failure
				cm.readyAt=cip.initDelay;
			}
			else {
				cm.readyAt = cip.initDelay*1000 + System.currentTimeMillis();
			}
			initParams.resourceState = ResourceStatus.INITIALIZING;
			setModel(cm);
		}
		// add acl entries
		ACLEntry e = new ACLEntry(OperationType.modify, "CN=Test", MatchType.DN);
		ACLEntry e1 = new ACLEntry(OperationType.read, "testers", MatchType.GROUP);
		cm.getAcl().add(e);
		cm.getAcl().add(e1);
		super.initialise(initParams);
	}

	@Override
	public void processMessages(PullPoint messageIterator) {
		while(messageIterator.hasNext()){
			messageIterator.next();
			processedMessages++;
		}
	}

	@Override
	public void activate() {
		if(getModel().readyAt==666) {
			getModel().setResourceStatus(ResourceStatus.ERROR);
			return;
		}
		long diff = getModel().readyAt -System.currentTimeMillis();
		if(diff<=0) {
			getModel().setResourceStatus(ResourceStatus.READY);
			getModel().setResourceStatusDetails("OK");
		}
	}

	public static class CounterInitParams extends InitParameters{

		public long initDelay;

		public CounterInitParams(String uuid){
			super(uuid);
		}

	}
}