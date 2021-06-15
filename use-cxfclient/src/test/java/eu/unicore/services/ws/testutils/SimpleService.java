	package eu.unicore.services.ws.testutils;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;

import eu.unicore.services.ws.AdminActionRequestDocument;
import eu.unicore.services.ws.AdminActionResponseDocument;
import eu.unicore.services.ws.GetServiceInstancesRequestDocument;
import eu.unicore.services.ws.GetServiceInstancesResponseDocument;

@WebService()
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface SimpleService {

	@WebMethod(action="fooRequest")
	public 
	@WebResult(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite", name="GetServiceInstancesResponse")
	GetServiceInstancesResponseDocument foo(
			@WebParam(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite", name="GetServiceInstancesRequest")
			GetServiceInstancesRequestDocument in);
	
	@WebMethod(action="adminRequest")
	public 
	@WebResult(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite", name="AdminActionResponse")
	AdminActionResponseDocument bar(
			@WebParam(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite", name="AdminActionRequest")
			AdminActionRequestDocument in);
	
}
