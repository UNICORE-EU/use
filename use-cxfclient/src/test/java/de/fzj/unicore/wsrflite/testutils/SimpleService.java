package de.fzj.unicore.wsrflite.testutils;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;

import de.fzj.unicore.wsrflite.xmlbeans.AdminActionRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesResponseDocument;

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
