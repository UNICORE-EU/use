package eu.unicore.services.ws;

import static org.junit.Assert.assertEquals;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.junit.Assert;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;

import de.fzj.unicore.wsrflite.WSRFConstants;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;

public class TestQnames {

	@Test
	public void test1(){

		assertEquals(WSRFConstants.RPcurrentTimeQName,
				CurrentTimeDocument.type.getDocumentElementName());

		assertEquals(WSRFConstants.RPterminationTimeQName,
				TerminationTimeDocument.type.getDocumentElementName());

	}

	@Test
	public void test2(){
		GetResourcePropertyDocument req=GetResourcePropertyDocument.Factory.newInstance();
		req.setGetResourceProperty(ResourceLifetime.RPcurrentTimeQName);
		System.out.println(req);
	}

	@Test
	public void test3(){
		QName qname=WSRFConstants.RPcurrentTimeQName;
		SchemaType st=XmlBeans.typeLoaderForClassLoader(getClass().getClassLoader()).findDocumentType(qname);
		XmlObject pDoc=XmlObject.Factory.newInstance().changeType(st);
		String content="foo";
		XmlCursor c=pDoc.newCursor();
		c.toFirstContentToken();
		c.insertElementWithText(qname, content);
		c.dispose();
		System.out.println(pDoc);
		Assert.assertTrue(pDoc instanceof CurrentTimeDocument);
		Assert.assertTrue(pDoc.toString().contains("foo"));
	}

}
