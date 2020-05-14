/*********************************************************************************
 * Copyright (c) 2008-2011 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
 

package de.fzj.unicore.wsrflite.xmlbeans;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;


/**
 * Remote administration service for USE. It offers
 * <ul>
 * <li>retrieval of metrics</li>
 * <li>invocation of admin actions</li>
 * </ul>
 * @author j.daivandy@fz-juelich.de
 * @author schuller
 */
@WebService(targetNamespace="http://www.fz-juelich.de/unicore/wsrflite/adminservice",
            portName = AdminService.SERVICE_NAME)
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface AdminService extends WSResource {
	
	public static final QName RPAdminServiceQName=new QName("http://www.fz-juelich.de/unicore/wsrflite","AdminServiceProperties");
	
	public static final QName RPServicesInfoQName=new QName("http://www.fz-juelich.de/unicore/wsrflite","ServicesInfo");		
	
	public static final QName RPServiceEntryQName=ServiceEntryDocument.type.getDocumentElementName();
	
	public static final QName RPMonitorEntryQName=MetricValueDocument.type.getDocumentElementName();

	public static final QName RPAdminActionsQName=AdminActionDocument.type.getDocumentElementName();

	public static final String SERVICE_NAME="AdminService";

	public static final String SINGLETON_ID = "default_admin";
	
	public static final String ADMINSERVICE_NS="http://www.fz-juelich.de/unicore/wsrflite/adminservice";
	
	public static final QName ADMINSERVICE_PORT=new QName(ADMINSERVICE_NS,SERVICE_NAME);
	
	public static final String NS="http://www.fz-juelich.de/unicore/wsrflite";
		
	/** Retrieves a list of all WS-Resource UIDs for the specified WSRF Web Service */	
	@WebMethod(action = "http://www.fz-juelich.de/unicore/wsrflite/GetServiceInstancesRequest")
	@WebResult(targetNamespace=NS, name="GetServiceInstancesResponse")
	public GetServiceInstancesResponseDocument getServiceInstances(
			@WebParam(targetNamespace=NS, name="GetServiceInstancesRequest")
			GetServiceInstancesRequestDocument req) throws BaseFault;	
	
	@WebMethod(action = "http://www.fz-juelich.de/unicore/wsrflite/GetMetrics")
	@WebResult(targetNamespace=NS, name="GetMetricsResponse")
	public GetMetricsResponseDocument getMetrics(
			@WebParam(targetNamespace=NS, name="GetMetricsRequest")
			GetMetricsRequestDocument req) throws BaseFault;
	
	@WebMethod(action = "http://www.fz-juelich.de/unicore/wsrflite/GetMetric")
	@WebResult(targetNamespace=NS, name="GetMetricResponse")
	public GetMetricResponseDocument getMetric(
			@WebParam(targetNamespace=NS, name="GetMetricRequest")
			GetMetricRequestDocument req)throws BaseFault;
	
	@WebMethod(action = "http://www.fz-juelich.de/unicore/wsrflite/InvokeAdminAction")
	@WebResult(targetNamespace=NS, name="AdminActionResponse")
	AdminActionResponseDocument invokeAdminAction(
			@WebParam(targetNamespace=NS, name="AdminActionRequest")
			AdminActionRequestDocument req)throws BaseFault;
	
	
}