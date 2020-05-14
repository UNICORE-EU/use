package de.fzj.unicore.wsrflite.xmlbeans;

import java.security.MessageDigest;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

/**
 * Renders a WS-Resource property as XML
 * 
 * @author schuller
 */
public interface XmlRenderer {

	/**
	 * Render the property into XML
	 * @return non-null array of XMLBeans documents
	 */
	public XmlObject[] render() throws Exception;
	
	/**
	 * render a part of the property into XML
	 * @param offset - the offset to start from
	 * @param length - the length
	 * @return list of resource property XML elements
	 */
	List<XmlObject> render(int offset, int length) throws Exception;
	
	/**
	 * get the total number of elements
	 * @throws Exception
	 */
	int getNumberOfElements() throws Exception;
	
	/**
	 * get the XML qualified name of the XML representation generated 
	 * by this renderer
	 */
	public QName getQName();
	
	/**
	 * compute a hash of the representation allowing to decide whether 
	 * the representation has changed
	 */
	public void updateDigest(MessageDigest md) throws Exception;

	/**
	 * marker interface that denotes XmlRenderer classes that should NOT be 
	 * published automatically 
	 */
	public static interface Internal {}

}
