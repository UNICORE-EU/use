package eu.unicore.services.ws.impl;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.XmlRenderer.Internal;
import eu.unicore.security.wsutil.client.ConditionalGetUtil;
import eu.unicore.services.ws.WSFrontEnd;
import eu.unicore.util.Log;

/**
 * Generates the WSRF resource property document for a Resource.<br>
 * 
 * @author schuller
 */
public class WSRFRepresentation {

	private static final Logger logger=Log.getLogger(Log.SERVICES, WSRFRepresentation.class);

	final WSFrontEnd wsrf;

	final Set<QName>propertyQNames;
	
	/**
	 * render all properties of the resource
	 * @param wsrf
	 */
	public WSRFRepresentation(WSFrontEnd wsrf){
		this.wsrf=wsrf;
		this.propertyQNames=wsrf.getRenderers().keySet();
	}

	/**
	 * render only the given set of resource properties
	 * @param wsrf - the WSRF frontend
	 * @param qnames - the qnames to render
	 */
	public WSRFRepresentation(WSFrontEnd wsrf, Set<QName>qnames){
		this.wsrf=wsrf;
		this.propertyQNames=qnames;
	}
	
	/**
	 * conditionally get the WSRF representation
	 * 
	 * @param clientETag - the ETag from the client
	 * @return full representation iff etags do not match, an empty representation otherwise
	 * @throws Exception
	 */
	public XmlObject conditionalGet(String clientETag) throws Exception{
		String myEtag=getETag();
		if(clientETag == null || myEtag == null || !myEtag.equals(clientETag)){
			return getContentObject();
		}
		else{
			return getEmptyContentObject();
		}
	}
	
	public XmlObject getContentObject() throws Exception {
		XmlObject reply=null;
		if(wsrf.getResourcePropertyDocumentQName()!=null){
			reply=createSchemaCompliantRPDocument();
		}
		else{
			reply=createNoSchemaRPDocument();
		}
		return reply;
	}
	
	public XmlObject getEmptyContentObject() throws Exception {
		XmlObject reply=null;
		if(wsrf.getResourcePropertyDocumentQName()!=null){
			reply=getWrapperDoc();
		}
		else{
			return XmlObject.Factory.parse("<unic:WSResourceImplResourceProperties " +
					"xmlns:unic=\"http://www.unicore.eu/unicore6/wsrflite\"/>");
		}
		return reply;
	}

	/**
	 * get a hash of the representation, allowing to decide whether the representation 
	 * has changed
	 * 
	 * @return etag or <code>null</code> if an error occurs
	 */
	public String getETag(){
		String result=null;
		try{
			MessageDigest md=MessageDigest.getInstance("MD5");
			for(QName q: propertyQNames){
				XmlRenderer xr=wsrf.getRenderer(q);
				if(xr!=null)xr.updateDigest(md);
			}
			result=ConditionalGetUtil.Server.hexString(md.digest());
		}catch(Exception ex){
			if(logger.isDebugEnabled()){
				logger.debug("Error computing digest", ex);
			}
		}
		return result;
	}

	/**
	 * puts all RPs into a XML document with a fixed root element name
	 */
	private XmlObject createNoSchemaRPDocument()throws Exception{
		XmlObject obj=XmlObject.Factory.parse("<unic:WSResourceImplResourceProperties xmlns:unic=\"http://www.unicore.eu/unicore6/wsrflite\"></unic:WSResourceImplResourceProperties>");
		XmlCursor c=obj.newCursor();
		while(!c.isStart())c.toNextToken();
		while(!c.isEnd())c.toNextToken();

		for(QName qn: propertyQNames){
			XmlRenderer renderer=wsrf.getRenderer(qn);
			if(renderer!=null && !(renderer instanceof Internal)){
				XmlObject[] os=wsrf.getResourcePropertyXML(qn);
				if(os!=null){
					for(XmlObject o:os){
						XmlCursor c1=o.newCursor();
						c1.toNextToken();
						c1.copyXml(c);
						c1.dispose();
					}
				}
			}
		}
		c.dispose();
		return obj;		
	}

	/**
	 * Creates the resource property document in a schema-compliant way, provided
	 * the {@link #getResourcePropertyDocumentQName()} returns a non-null value.<br>
	 * Additional resource properties are appended to the document via 
	 * the {@link #appendAdditionalRPs(XmlObject, Set)} method, which is called
	 * after the schema-compliant doc has been created
	 */
	private XmlObject createSchemaCompliantRPDocument(){
		try{
			Set<QName>remainingQNames=new HashSet<QName>();
			remainingQNames.addAll(propertyQNames);
			//make the base element
			ClassLoader cl = wsrf.getClass().getClassLoader();
			SchemaType st=XmlBeans.typeLoaderForClassLoader(cl).findDocumentType(wsrf.getResourcePropertyDocumentQName());
			SchemaProperty[] sp=st.getElementProperties();
			String pName=sp[0].getJavaPropertyName();
			XmlObject pDoc=XmlObject.Factory.newInstance().changeType(st);
			Method adder=pDoc.getClass().getMethod("addNew"+pName, new Class[]{});
			//the properties base element
			XmlObject props=(XmlObject)adder.invoke(pDoc, (Object[])null);
			XmlCursor c=props.newCursor();
			c.toFirstContentToken();
			//fill the element
			sp=props.schemaType().getElementProperties();
			XmlObject lastRP=null;
			for(SchemaProperty p: sp){
				try {
					QName qname=p.getName();
					remainingQNames.remove(qname);
					XmlRenderer xr=wsrf.getRenderer(qname);
					if(xr == null || xr instanceof XmlRenderer.Internal)continue;
					XmlObject[] rps=xr.render();
					if(rps==null)continue;
					for(XmlObject o: rps){
						if(o!=null){
							lastRP=o;
							XmlCursor c2=o.newCursor();
							c2.toFirstChild();
							c2.copyXml(c);
							c2.dispose();
						}
						else{
							logger.debug("Null xml for property "+qname);
						}
					}
				} catch (Exception e) {
					Log.logException("Exception setting value for resource property"+p.getName(),e,logger);
				}
				catch(Error err){
					logger.error("Error setting value for resource property "+p.getName(),err);
					try{
						logger.error("Value : "+lastRP);
					}catch(Throwable ignored){}
				}
			}
			c.dispose();

			//add non-schema RPs if required
			appendAdditionalRPs(pDoc, remainingQNames);

			return pDoc;
		}catch(Exception e){
			Log.logException("Can't build rp document.",e,logger);
		}
		return null;
	}

	private XmlObject getWrapperDoc()throws Exception{
		ClassLoader cl = wsrf.getClass().getClassLoader();
		SchemaType st=XmlBeans.typeLoaderForClassLoader(cl).findDocumentType(wsrf.getResourcePropertyDocumentQName());
		SchemaProperty[] sp=st.getElementProperties();
		String pName=sp[0].getJavaPropertyName();
		XmlObject pDoc=XmlObject.Factory.newInstance().changeType(st);
		Method adder=pDoc.getClass().getMethod("addNew"+pName, new Class[]{});
		//the properties base element
		adder.invoke(pDoc, (Object[])null);
		return pDoc;
	}

	/**
	 * if your properties map contains additional ResourceProperties that are NOT defined by the 
	 * resource property document schema, this method will append them to the rp document anyway
	 * provided they do NOT implement the Hidden interface
	 * @param propertyDoc - the resource property document created according to the schema
	 * @param remainingQNames - the qnames of non-schema properties
	 * @throws Exception
	 */
	protected void appendAdditionalRPs(XmlObject propertyDoc, Set<QName> remainingQNames)throws Exception{
		for(QName q: remainingQNames){
			XmlRenderer prop=wsrf.getRenderer(q);
			if(prop!=null && !(prop instanceof Internal)){
				XmlObject[]o=prop.render();
				if(o!=null){
					WSUtilities.append(o,propertyDoc);
				}
				else{
					logger.debug("NULL RP "+q+" for resource "
							+wsrf.getResource().getServiceName()
							+"<"+wsrf.getResource().getUniqueID()+">");
				}
			}
		}
	}
}
