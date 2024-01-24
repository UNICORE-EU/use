package eu.unicore.services.pdp.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.herasaf.xacml.core.context.RequestMarshaller;
import org.herasaf.xacml.core.context.impl.AttributeType;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.junit.Test;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.pdp.MockAuthZContext;
import eu.unicore.services.pdp.request.creator.HerasafXacml2RequestCreator;
import eu.unicore.services.pdp.request.profile.UnicoreInternalProfile;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.ResourceDescriptor;


public class RequestMakerTest
{
	@Test
	public void testAnonymous()
	{
		SimplePDPFactory.getSimplePDP();
		HerasafXacml2RequestCreator reqMaker = new HerasafXacml2RequestCreator(
			new UnicoreInternalProfile("http://localhost"));
		Client c = new Client();
		ActionDescriptor action = new ActionDescriptor("", null);
		ResourceDescriptor des = new ResourceDescriptor(
				"http://serviceName", "default_resource", 
				"CN=Testing Tester,C=XX");
		try
		{
			reqMaker.setAllowAnonymous(false);
			reqMaker.createRequest(c, action, des);
			fail("Managed to create request with empty client");
		} catch (Exception e)
		{
			assertTrue(e instanceof IllegalArgumentException);
		}
		reqMaker.setAllowAnonymous(true);
		reqMaker.createRequest(c, action, des);
	}

	@Test
	public void testFull()
	{
		try
		{
			SimplePDPFactory.getSimplePDP();
			HerasafXacml2RequestCreator reqMaker = new HerasafXacml2RequestCreator(
				new UnicoreInternalProfile("http://localhost"));
			Client c = MockAuthZContext.createRequest("user", 
					"CN=Testing Tester,C=XX");
			ActionDescriptor action = new ActionDescriptor("testAction", OperationType.modify);
			ResourceDescriptor des = new ResourceDescriptor(
					"http://serviceName", "default_resource", 
					"CN=Testing Owner,C=XX");
			
			List<XACMLAttribute> ret = new ArrayList<XACMLAttribute>();
			ret.add(new XACMLAttribute("a1", "true", XACMLAttribute.Type.BOOL));
			ret.add(new XACMLAttribute("a2", "hello", XACMLAttribute.Type.STRING));
			SubjectAttributesHolder subjectAttrs = new SubjectAttributesHolder();
			subjectAttrs.setXacmlAttributes(ret);
			c.setSubjectAttributes(subjectAttrs);
			
			RequestType req = reqMaker.createRequest(c, action, des);
			RequestMarshaller.marshal(req,System.out);
			
			assertEquals(2, req.getAction().getAttributes().size());
			assertTrue(req.getAction().getAttributes().get(0).getAttributeId().equals(XACMLAttribute.Name.XACML_ACTION_ID_ATTR.toString()) &&
				req.getAction().getAttributes().get(0).getAttributeValues().get(0).getContent().get(0).equals("testAction"));
			assertTrue(req.getAction().getAttributes().get(1).getAttributeId().equals(UnicoreInternalProfile.ATTR_ACTION_TYPE_ID) &&
					req.getAction().getAttributes().get(1).getAttributeValues().get(0).getContent().get(0).equals(OperationType.modify.toString()));
				
			assertTrue(req.getSubjects().size() == 1);
			List<AttributeType> subAttrs = req.getSubjects().get(0).getAttributes();
			assertEquals(6, subAttrs.size());

			assertTrue(subAttrs.get(0).getAttributeId().equals(XACMLAttribute.Name.XACML_SUBJECT_ID_ATTR.toString()) &&
					subAttrs.get(0).getAttributeValues().get(0).getContent().get(0).equals(X500NameUtils.getComparableForm("CN=Testing Tester,C=XX")));
			assertTrue(subAttrs.get(1).getAttributeId().equals(UnicoreInternalProfile.ATTR_ROLE_XACML_ID) &&
					subAttrs.get(1).getAttributeValues().get(0).getContent().get(0).equals("user"));
			assertTrue(subAttrs.get(2).getAttributeId().equals(UnicoreInternalProfile.ATTR_SUBJECT_CONSIGNOR_XACML_ID) &&
					subAttrs.get(2).getAttributeValues().get(0).getContent().get(0).equals(X500NameUtils.getComparableForm("CN=TestServer,OU=ICM,O=UW,L=Warsaw,ST=Unknown,C=PL")));
			assertTrue(subAttrs.get(3).getAttributeId().equals("aclCheckPassed") &&
					subAttrs.get(3).getAttributeValues().get(0).getContent().get(0).equals("false"));
			assertTrue(subAttrs.get(4).getAttributeId().equals("a1") &&
					subAttrs.get(4).getAttributeValues().get(0).getContent().get(0).equals("true"));
			assertTrue(subAttrs.get(5).getAttributeId().equals("a2") &&
					subAttrs.get(5).getAttributeValues().get(0).getContent().get(0).equals("hello"));	
			
			assertTrue(req.getResources().size() == 1);
			List<AttributeType> resAttrs = req.getResources().get(0).getAttributes();
			assertEquals(3,resAttrs.size());
			assertTrue(resAttrs.get(0).getAttributeId().equals(UnicoreInternalProfile.ATTR_WSR_XACML_ID) &&
					resAttrs.get(0).getAttributeValues().get(0).getContent().get(0).equals("default_resource"));
			assertTrue(resAttrs.get(1).getAttributeId().equals(XACMLAttribute.Name.XACML_RESOURCE_ID_ATTR.toString()) &&
					resAttrs.get(1).getAttributeValues().get(0).getContent().get(0).equals("http://serviceName"));
			assertTrue(resAttrs.get(2).getAttributeId().equals(UnicoreInternalProfile.ATTR_RESOURCE_OWNER_XACML_ID) &&
					resAttrs.get(2).getAttributeValues().get(0).getContent().get(0).equals(X500NameUtils.getComparableForm("CN=Testing Owner,C=XX")));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
}
