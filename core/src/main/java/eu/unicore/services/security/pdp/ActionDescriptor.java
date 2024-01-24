package eu.unicore.services.security.pdp;

import eu.unicore.security.OperationType;


/**
 * Describes an action for the PDP
 * @author K. Benedyczak
 */
public class ActionDescriptor {
	private final String action;
	private final OperationType actionType;
	
	public ActionDescriptor(String action, OperationType actionType) {
		this.action = action;
		this.actionType = actionType;
	}

	public String getAction() {
		return action;
	}

	public OperationType getActionType() {
		return actionType;
	}
	
	public String toString() {
		return action + " [accessType=" + actionType + "]";
	}
}
