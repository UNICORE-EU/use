package eu.unicore.services.ws;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

/**
 * convenient base class for renderers
 * 
 * @author schuller
 */
public abstract class AbstractXmlRenderer implements XmlRenderer {

	protected final QName qName;

	public AbstractXmlRenderer(QName qname){
		this.qName=qname;
	}
	
	public QName getQName(){
		return qName;
	}

	/**
	 * returns a subset of the resource property xml.
	 * <br>
	 * NOTE: subclasses should provide an optimized implementation for this method
	 * 
	 * @param offset - the offset to start from
	 * @param length - the (max) number of results to return
	 * @return list of resource property XML elements
	 * @throws IndexOutOfBoundsException - if the offset is larger than the number of
	 * available results
	 */
	public List<XmlObject> render(int offset, int length)throws Exception{
		List<XmlObject>result=new ArrayList<XmlObject>();
		XmlObject[]all=render();
		if(offset>=all.length)throw new IndexOutOfBoundsException("Requested offset is too large, only have <"+all.length+"> results.");
		for(int i=0;i<length && offset+i < all.length;i++){
			result.add(all[offset+i]);
		}
		return result;
	}

	/**
	 * gets the total number of elements returned by render()
	 * <br>
	 * NOTE: subclasses should provide an optimized implementation for this method
	 */
	public int getNumberOfElements() throws Exception{
		return render().length;
	}
	
}
