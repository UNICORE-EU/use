package de.fzj.unicore.wsrflite.xmlbeans;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.FixedAddressRenderer;

public class TestAddressRenderer {

	@Test
	public void test1()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.WSRF_BASEURL, "http://foo");
		Kernel kernel=new Kernel(p);
		Resource r=new MockWSResourceImpl();
		r.setKernel(kernel);
		final String s="testService?res=123";
		QName docName=EndpointReferenceDocument.type.getDocumentElementName();
		AddressRenderer rp=new FixedAddressRenderer(r,docName,s,false);
		EndpointReferenceDocument x=(EndpointReferenceDocument)rp.render()[0];
		String address=x.getEndpointReference().getAddress().getStringValue();
		assertEquals("http://foo/testService?res=123",address);
		
	}
}
