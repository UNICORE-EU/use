package eu.unicore.services.ws;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

import eu.unicore.services.Resource;

/**
 * WSFrontEnd implementations handle web service invocations on a {@link Resource}.
 * 
 * NOTE: implementations <b>must</b> have a constructor with a Resource-typed parameter
 * 
 * @author schuller
 */
public interface WSFrontEnd {

	public Resource getResource();
	
	public void addRenderer(XmlRenderer r);
	
	public void addRenderer(QName q, XmlRenderer r);
	
	public XmlRenderer getRenderer(QName qn);

	public QName getResourcePropertyDocumentQName();

	public QName getPortType();

	public XmlObject[] getResourcePropertyXML(QName qname);

	public Map<QName, XmlRenderer> getRenderers();

	public XmlObject getResourcePropertyResponseDocument() throws Exception;

}
