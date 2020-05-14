package de.fzj.unicore.wsrflite.persistence;

/**
 * Load semantics specify the behaviour of the persistence layer with
 * respect to loading ws-resources
 *
 * @author schuller
 */
public enum LoadSemantics {

	/**
	 * use the "normal" way of persisting instances:
	 * the WS Resource is loaded into memory only for the duration 
	 * of the request
	 */
	NORMAL,
	
	/**
	 * load the WS-Resource into memory, and keep it there for the lifetime of the server
	 * Changes to the instance will be persisted to disk
	 */
	LOAD_ONCE,
	
}
