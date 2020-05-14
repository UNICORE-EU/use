package de.fzj.unicore.wsrflite.xmlbeans.renderers;

import java.math.BigInteger;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.wsrflite.exceptions.InvalidModificationException;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.AbstractXmlRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.Modifiable;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.Log;

/**
 * A XML representation that can be edited through standard WSRF functions.
 * 
 * @author schuller
 */
public abstract class ModifiableBase<T extends XmlObject> extends AbstractXmlRenderer implements Modifiable<T>{

	protected static final Logger logger=Log.getLogger(Log.SERVICES,ModifiableBase.class);
	
	protected boolean checkSchema;
	
	protected BigInteger minOccurs=BigInteger.ONE;
	
	protected BigInteger maxOccurs=BigInteger.ONE;
	
	protected final ResourceImpl parent;

	/**
	 * Use this constructor if schema definition of the RP is not in the same XSD as 
	 * the service. You have to pass cardinality of the RP elements (it won't be used if
	 * you disable schema check validation).
	 * @param type type of the RP
	 * @param parent parent WS resource
	 * @param checkSchema whether to check cardinality of content elements
	 * @param min minimum number of content elements
	 * @param max maximum number of content elements
	 */
	public ModifiableBase(QName type, ResourceImpl parent, boolean checkSchema, int min, int max) {
		super(type);
		this.parent=parent;
		this.checkSchema=checkSchema;
		minOccurs = BigInteger.valueOf(min);
		maxOccurs = BigInteger.valueOf(max);
	}
	
	/**
	 * This constructor tries to extract schema information from the XSD document
	 * of the parent WS resource. 
	 * @param type type of the RP
	 * @param parent parent WS resource
	 * @param checkSchema whether to check cardinality of content elements
	 */
	public ModifiableBase(QName type, WSResourceImpl parent, boolean checkSchema){
		super(type);
		this.parent=parent;
		this.checkSchema=checkSchema;
		if(checkSchema)extractSchemaInfo(type, parent);
	}
	
	/**
	 * Creates RP without checking of content elements cardinality.
	 * @param type type of the RP
	 * @param parent parent WS resource
	 */
	public ModifiableBase(QName type, WSResourceImpl parent){
		this(type,parent,false);
	}
	
	private void extractSchemaInfo(QName type, WSResourceImpl parent) {
		try{
			QName propertiesDocumentType=parent.getResourcePropertyDocumentQName();
			//find schematype corresponding to the resourceproperties documement
			SchemaType rpDocSchemaType=XmlBeans.getContextTypeLoader().findDocumentType(propertiesDocumentType);
			if(rpDocSchemaType!=null){
				SchemaProperty sp=rpDocSchemaType.getElementProperty(type);
				if(sp!=null){
					minOccurs=sp.getMinOccurs();
					maxOccurs=sp.getMaxOccurs();
				}
				else
					logger.warn("No schema type found for type "+type+" in properties document "+propertiesDocumentType);
			}
			else
				logger.warn("No schema type found for QName "+propertiesDocumentType);
		}catch(Exception e){
			logger.warn("Can't setup resource property: ", e);
		}
	}

	@Override
	public abstract T[] render();

	public void insert(T o)throws InvalidModificationException{
		if(checkSchema)
			checkSchema(getCurrentSize()+1);
		doAdd(o);
	}
	
	public void update(List<T> o)throws InvalidModificationException{
		if(checkSchema)
			checkSchema(o.size());
		doClear();
		
		for(T t: o)doAdd(t);
	}
	
	public void delete()throws InvalidModificationException{
		if(checkSchema)
			checkSchema(0);
		doClear();
	}
	
	/**
	 * get the current number of elements
	 */
	public abstract int getCurrentSize();
	
	/**
	 * clear everything
	 */
	protected abstract void doClear();
	
	/**
	 * add the given value
	 * @param o - the XML representation of the value to add
	 */
	protected abstract void doAdd(T o);
	
	
	/**
	 * checks whether the given element cardinality is allowed by the schema 
	 * 
	 * @param cardinality
	 * @return
	 */
	protected void checkSchema(int cardinality) throws InvalidModificationException{
		BigInteger c=BigInteger.valueOf(cardinality);
		if (minOccurs.compareTo(c)>0 || c.compareTo(maxOccurs)<0)
			throw new InvalidModificationException("The number of resource property " +
					"elements must be between " + minOccurs + " and " + maxOccurs);
	}
	
	/**
	 * check whether an insert/update/delete operation is allowed
	 *  
	 * @param permissions
	 * @return
	 */
	public boolean checkPermissions(int permissions){
		return true;
	}
	
}
