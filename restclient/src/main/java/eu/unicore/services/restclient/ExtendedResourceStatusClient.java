package eu.unicore.services.restclient;

import java.util.concurrent.TimeoutException;

/**
 * client side interface for resources with an extended lifecycle
 */
public interface ExtendedResourceStatusClient {

	/**
	 * make sure the resource is ready
	 *
	 * @param timeout in seconds (-1 for no timeout)
	 * @throws Exception on errors
	 * @throws TimeoutException if waiting times out
	 */
	public default void assertReady(int timeout) throws Exception {
		int i=0;
		while(i < timeout) {
			ResourceStatus status = getResourceStatus();
			switch (status) {
			case READY:
				return;
			case INITIALIZING:
			case UNDEFINED:
			{
				i++;
				Thread.sleep(1000);
				break;
			}
			default:
				throw new Exception("Resource status is <"+status+">");
			}
		}
		throw new TimeoutException("Timeout waiting for resource to become ready");
	}

	public ResourceStatus getResourceStatus() throws Exception;

	/**
	 * Enumerate the different resource stati
	 * same as server side ExtendedResourceStatus.ResourceStatus
	 */
	public static enum ResourceStatus {
		UNDEFINED,
		INITIALIZING,
		READY,
		DISABLED,
		ERROR,
		SHUTTING_DOWN,
	};
}