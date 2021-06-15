package eu.unicore.services.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.oasisOpen.docs.wsrf.rl2.CurrentTimeDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.services.ws.MockWSResourceImpl.ModifyRenderer;
import eu.unicore.services.ws.renderers.AddressListRenderer;
import eu.unicore.services.ws.renderers.FieldRenderer;

public class TestRenderers {

	@Test
	public void testAddressListSingle()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		Kernel kernel=new Kernel(p);
		MockWSResourceImpl r=new MockWSResourceImpl();
		r.setKernel(kernel);
		r.setModel(new MockResourceModel());
		String s="testService";
		QName docName=EndpointReferenceDocument.type.getDocumentElementName();
		QName portType=new QName("foo","bar");
		AddressListRenderer rp=new AddressListRenderer(r,s,docName,portType,false){
			protected List<String>getUIDs(){
				return ((MockWSResourceImpl)parent).getChildIDs();
			}
		};
		
		r.getChildIDs().add("test123");
		XmlObject[]x=rp.render();
		assertEquals(1, x.length);
		EndpointReferenceDocument epr=(EndpointReferenceDocument)x[0];
		String url=epr.getEndpointReference().getAddress().getStringValue();
		assertTrue(url.equals("http://foo/services/testService?res=test123"));
	}
	
	@Test
	public void testAddressListMany()throws Exception{
		String s="testService";
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		Kernel kernel=new Kernel(p);
		MockWSResourceImpl r=new MockWSResourceImpl();
		r.setKernel(kernel);
		r.setModel(new MockResourceModel());
		QName docName=EndpointReferenceDocument.type.getDocumentElementName();
		QName portType=new QName("foo","bar");
		AddressListRenderer rp=new AddressListRenderer(r,s,docName,portType,false){
			protected List<String>getUIDs(){
				return ((MockWSResourceImpl)parent).getChildIDs();
			}
		};
		int N=200;
		for(int i=0; i<N; i++){
			r.getChildIDs().add("test"+i);
		}
		XmlObject[]x=rp.render();
		assertEquals(N, x.length);
		EndpointReferenceDocument epr=(EndpointReferenceDocument)x[0];
		String url=epr.getEndpointReference().getAddress().getStringValue();
		assertTrue(url.equals("http://foo/services/testService?res=test0"));
		
		//test getting a subset
		List<XmlObject>x2=rp.render(10,10);
		assertEquals(10, x2.size());
		epr=(EndpointReferenceDocument)x2.get(0);
		url=epr.getEndpointReference().getAddress().getStringValue();
		assertTrue(url.equals("http://foo/services/testService?res=test10"));
		
	}
	
	@Test
	public void testFieldRenderer()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		p.setProperty("persistence.directory","target/data");
		Kernel kernel=new Kernel(p);
		MockWSResourceImpl r=new MockWSResourceImpl();
		r.setKernel(kernel);
		r.initialise(new InitParameters());
		
		// single value
		FieldRenderer fieldRenderer=(FieldRenderer)r.getRenderer(CurrentTimeDocument.type.getDocumentElementName());
		XmlObject[]xml=fieldRenderer.render();
		assertEquals(1, xml.length);
		assertTrue(xml[0] instanceof CurrentTimeDocument);
		System.out.println(Arrays.asList(xml));
		
		// set
		FieldRenderer fieldRenderer2=(FieldRenderer)r.getRenderer(new QName("a","b"));
		XmlObject[]xml2=fieldRenderer2.render();
		assertEquals(3, xml2.length);
		System.out.println(Arrays.asList(xml2));
		
		// array
		FieldRenderer fieldRenderer3=(FieldRenderer)r.getRenderer(new QName("array","b"));
		XmlObject[]xml3=fieldRenderer3.render();
		assertEquals(3, xml3.length);
		System.out.println(Arrays.asList(xml3));
		
		// primitive array
		FieldRenderer fieldRenderer4=(FieldRenderer)r.getRenderer(new QName("intarray","b"));
		XmlObject[]xml4=fieldRenderer4.render();
		assertEquals(3, xml4.length);
		System.out.println(Arrays.asList(xml4));
	}
	
	@Test
	public void testModify()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		p.setProperty("persistence.directory","target/data");
		Kernel kernel=new Kernel(p);
		MockWSResourceImpl r=new MockWSResourceImpl();
		r.setKernel(kernel);
		r.initialise(new InitParameters());
		
		QName q=new QName("tags","tag");
		Modifiable<XmlObject> mod=(ModifyRenderer)r.getRenderer(q);
		XmlRenderer renderer=r.getRenderer(q);
		XmlObject[] tags=renderer.render();
		System.out.println(Arrays.asList(tags));
		assertEquals(0,tags.length);
		XmlObject i1=WSUtilities.createXmlDoc(q, "tag1", null);
		System.out.println(" >>> inserting "+i1);
		mod.insert(i1);
		tags=renderer.render();
		System.out.println(" <<<" +Arrays.asList(tags));
		assertEquals(1,tags.length);
		XmlObject i2=WSUtilities.createXmlDoc(q, "tag2", null);
		System.out.println(" >>> inserting "+i2);
		mod.insert(i2);
		tags=renderer.render();
		System.out.println(" <<<" +Arrays.asList(tags));
		assertEquals(2,tags.length);
		System.out.println(" >>> deleting");
		mod.delete();
		tags=renderer.render();
		System.out.println(" <<<" +Arrays.asList(tags));
		assertEquals(0,tags.length);
		List<XmlObject>list=new ArrayList<XmlObject>();
		list.add(i1);
		list.add(i2);
		System.out.println(" >>> updating with "+list);
		mod.update(list);
		tags=renderer.render();
		System.out.println(" <<<" +Arrays.asList(tags));
		assertEquals(2,tags.length);
	}
	
}
