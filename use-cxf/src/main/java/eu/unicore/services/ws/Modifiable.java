package eu.unicore.services.ws;

import java.util.List;

import eu.unicore.services.exceptions.InvalidModificationException;

/**
 * provides methods to update representations
 * 
 * @author schuller
 */
public interface Modifiable<T> {
	
	public static final int INSERT=1;
	
	public static final int DELETE=2;
	
	public static final int UPDATE=4;
	
	
	public void insert(T o)throws InvalidModificationException;
	
	public void update(List<T> o)throws InvalidModificationException;
	
	public void delete()throws InvalidModificationException;
	
	/**
	 * check whether an insert/update/delete operation is allowed
	 *  
	 * @param permissions
	 * @return
	 */
	public boolean checkPermissions(int permissions);
	
}
