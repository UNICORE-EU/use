package de.fzj.unicore.wsrflite.xmlbeans.renderers;

import java.lang.reflect.Field;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.wsrflite.Model;
import de.fzj.unicore.wsrflite.Resource;

/**
 * Renders the value of a fixed field of the WS-Resource Model. 
 * Can handle single values as well as arrays and collections, 
 * where the value can be an XmlObject or any other type.
 * 
 * @author schuller
 */
public class FieldRenderer extends ValueRenderer {

	private final String fieldName;
	
	/**
	 * 
	 * @param parent - the parent resource
	 * @param docName - the QName of the generated XML
	 * @param fieldName - the name of the field
	 */
	public FieldRenderer(Resource parent, QName docName, String fieldName){
		super(parent,docName);
		this.fieldName=fieldName;
	}
	
	@Override
	protected Object getValue()throws NoSuchFieldException, IllegalAccessException{
		Model model = parent.getModel();
		Field f=getField(model.getClass());
		f.setAccessible(true);
		return f.get(model);
	}
	
	@SuppressWarnings("rawtypes")
	private Field getField(Class clazz) throws NoSuchFieldException{
		if(Object.class.equals(clazz))return null;
		for(Field fi: clazz.getDeclaredFields()){
			if(fieldName.equals(fi.getName()))return fi;
		}
		//check superclass
		return getField(clazz.getSuperclass());
	}

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

}
