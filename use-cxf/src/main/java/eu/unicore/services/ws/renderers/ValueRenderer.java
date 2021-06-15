package eu.unicore.services.ws.renderers;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.services.Resource;
import eu.unicore.services.ws.AbstractXmlRenderer;

/**
 * Renders some value, which is retrieved using the getValue() method 
 * Can handle single values as well as arrays and collections, 
 * where the value can be an XmlObject or any other type.
 * 
 * The result is cached, so subsequent invocations of render() will be fast
 * 
 * @author schuller
 */
public abstract class ValueRenderer extends AbstractXmlRenderer {

	protected final Resource parent;

	// result is only cached between calls do updateDigest() and render()
	// not between subsequent render() calls
	private XmlObject[] cachedResult;

	/**
	 * 
	 * @param parent - the parent resource
	 * @param docName - the QName of the generated XML
	 */
	public ValueRenderer(Resource parent, QName docName){
		super(docName);
		this.parent=parent;
	}

	/**
	 * use this constructor only if getValue() already returns XML documents
	 * 
	 * @param parent - the parent resource
	 */
	public ValueRenderer(Resource parent){
		this(parent,null);
	}

	@Override
	public XmlObject[] render() throws Exception {
		try{
			if(cachedResult!=null){
				return cachedResult;
			}
			else{
				return doRender();
			}
		}finally{
			cachedResult=null;
		}
	}
	
	private XmlObject[] doRender()throws Exception {
		XmlObject[]res=null;
		Object value = getValue();
		if(value == null){
			res = new XmlObject[0];
		}
		else{
			boolean isArray=value.getClass().isArray();

			if(isArray){
				res = renderArray(value);
			}
			else{
				Collection<?>values = null;
				if(value instanceof Collection){
					values = ((Collection<?>)value);
				}
				else{
					values = Arrays.asList(value);
				}
				res = renderCollection(values);
			}
		}
		return res;
	}

	private XmlObject[] renderArray(Object array){
		int length=Array.getLength(array);
		XmlObject[] result=new XmlObject[length];
		for(int i=0; i<length;i++){
			Object value=Array.get(array, i);
			result[i]=renderValue(value);
		}
		return result;
	}

	private XmlObject[] renderCollection(Collection<?>values){
		if(values==null)return new XmlObject[0];

		XmlObject[] result=new XmlObject[values.size()];
		int i=0;
		for(Object v: values){
			result[i]=renderValue(v);
			i++;
		}
		return result;
	}

	protected XmlObject renderValue(Object v){
		XmlObject pDoc=null;
		if(v instanceof XmlObject){
			pDoc=(XmlObject)v;
		}
		else{
			String content=String.valueOf(v);
			pDoc=createXmlDoc(content);
		}
		return pDoc;
	}

	/**
	 * get the value to render
	 * @throws Exception
	 */
	protected abstract Object getValue() throws Exception;

	protected XmlObject createXmlDoc(String content){
		ClassLoader cl = parent.getClass().getClassLoader();
		SchemaType st=XmlBeans.typeLoaderForClassLoader(cl).findDocumentType(qName);
		XmlObject pDoc=XmlObject.Factory.newInstance();
		if(st!=null){
			pDoc.changeType(st);
		}
		XmlCursor c=pDoc.newCursor();
		c.toFirstContentToken();
		c.insertElementWithText(qName, content);
		c.dispose();
		return pDoc;
	}

	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		cachedResult=doRender();
		for(XmlObject o: cachedResult){
			md.update(String.valueOf(o).getBytes());
		}
	}

}
