package de.fzj.unicore.wsrflite.xmlbeans.client;

import java.util.List;

import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xfire.ClientException;

/**
 * query methods on a registry / service group
 * 
 * @author schuller
 */
public interface IRegistryQuery {

	/**
	 * list services implementing a given porttype<br>
	 * 
	 * this uses XPath to query the registry, and relies on the
	 * entry having the structure<br>
	 * <code>
	 *  sg: ServiceGroupRP/sg:Entry/sg:MemberServiceEPR/ad:Metadata/meta:InterfaceName
	 *</code>
	 * with an ws addressing metadata entry listing the "final porttype" that the service implements
	 * @param porttype - QName of the porttype to look for
	 * @return List of EPRs
	 */
	public List<EndpointReferenceType> listServices(QName porttype)	throws Exception;

	/**
	 * 
	 * @param porttype -  the porttype
	 * @param acceptFilter -  for filtering the list
	 * @return List of EPRs
	 * @throws Exception
	 */
	public List<EndpointReferenceType> listServices(QName porttype,	ServiceListFilter acceptFilter) throws Exception;

	/**
	 * list services implementing a given porttype<br> that are accessible to the
	 * current client (using getCurrentTime() as test)
	 * 
	 * @param porttype
	 * @return List of EPRs
	 * @throws Exception
	 */
	public List<EndpointReferenceType> listAccessibleServices(QName porttype) throws Exception;

	/**
	 * list the entries in this registry
	 */
	public abstract List<EntryType> listEntries()throws Exception;
	
	/**
	 * allows to specify custom filtering conditions on the service list
	 * returned by the registry client
	 */
	public interface ServiceListFilter {
		public boolean accept(EntryType epr);
	}
	
	public static final String STATUS_OK="OK";
	
	/**
	 * check the connection status to the service
	 * 
	 * @return STATUS_OK (i.e. the string "OK") if connection is OK, 
	 *         an error message otherwise
	 */
	public String getConnectionStatus()throws ClientException;	
	
	/**
	 * check the connection to the service
	 * @return true if service can be accessed
	 */
	public boolean checkConnection()throws ClientException;	
	
	/**
	 * check the connection to the WSRF service by calling getCurrentTime().
	 * If the service does not reply within the given timeout, returns <code>false</code>
	 * 
	 * @param timeout - connection timeout in milliseconds
	 * @return false in case of problems contacting the remote service
	 * @throws ClientException in case of client-side errors
	 */
	public boolean checkConnection(int timeout)throws ClientException;

}