package eu.unicore.services.ws;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceDocument;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.services.ws.renderers.AddressRenderer;
import eu.unicore.services.ws.renderers.FixedAddressRenderer;

public class TestAddressRenderer {

	@Test
	public void test1()throws Exception{
		Properties p = TestConfigUtil.getInsecureProperties();
		p.setProperty(ContainerProperties.PREFIX+ContainerProperties.EXTERNAL_URL, "http://foo");
		Kernel kernel=new Kernel(p);
		Resource r=new MockWSResourceImpl();
		r.setKernel(kernel);
		final String s="testService?res=123";
		QName docName=EndpointReferenceDocument.type.getDocumentElementName();
		AddressRenderer rp=new FixedAddressRenderer(r,docName,s,false);
		EndpointReferenceDocument x=(EndpointReferenceDocument)rp.render()[0];
		String address=x.getEndpointReference().getAddress().getStringValue();
		assertEquals("http://foo/services/testService?res=123",address);
		
	}
}
