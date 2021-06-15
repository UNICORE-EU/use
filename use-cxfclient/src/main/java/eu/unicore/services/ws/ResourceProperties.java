/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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


package eu.unicore.services.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;

import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteChildResourcesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.DeleteResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.GetMultipleResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.GetResourcePropertyResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.InsertResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentDocument1;
import org.oasisOpen.docs.wsrf.rp2.PutResourcePropertyDocumentResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.QueryResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.SetResourcePropertiesResponseDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesDocument;
import org.oasisOpen.docs.wsrf.rp2.UpdateResourcePropertiesResponseDocument;

import eu.unicore.services.WSRFConstants;
import eu.unicore.services.ws.exceptions.InvalidResourcePropertyQNameFault;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;

/**
 * ResourceProperties interface
 */

@WebService(targetNamespace=WSRFConstants.WSRP_BASENS)
@SOAPBinding(style=Style.DOCUMENT, use=Use.LITERAL, parameterStyle=ParameterStyle.BARE)
public interface ResourceProperties extends WSRFConstants {

	/**
	 * the wsa:Action for GetResourcePropertyDocument 
	 */
	public static final String WSRP_GET_RP_DOCUMENT=WSRP_BASENS+"/GetResourcePropertyDocument/GetResourcePropertyDocumentRequest";

	/**
	 * the wsa:Action for GetResourceProperty
	 */
	public static final String WSRP_GET_RP=WSRP_BASENS+"/GetResourceProperty/GetResourcePropertyRequest";

	/**
	 * the wsa:Action for GetMultipleResourceProperties
	 */
	public static final String WSRP_GET_MULTIPLE_RP=WSRP_BASENS+"/GetMultipleResourceProperties/GetMultipleResourcePropertiesRequest";

	/**
	 * the wsa:Action for QueryResourceProperties
	 */
	public static final String WSRP_QUERY_RP=WSRP_BASENS+"/QueryResourceProperties/QueryResourcePropertiesRequest";

	/**
	 * the QueryExpressionDialect ResourceProperty
	 */
	public static final QName WSRP_RP_QueryExpressionDialect=new QName(WSRP_BASENS,"QueryExpressionDialect");

	/**
	 * the wsa:Action for PutResourcePropertyDocument 
	 */
	public static final String WSRP_PUT_RP_DOCUMENT=WSRP_BASENS+"/PutResourcePropertyDocument/PutResourcePropertyDocumentRequest";

	/**
	 * the wsa:Action for InsertResourceProperties
	 */
	public static final String WSRP_SET_RP=WSRP_BASENS+"/SetResourceProperties/SetResourcePropertiesRequest";

	/**
	 * the wsa:Action for InsertResourceProperties
	 */
	public static final String WSRP_INSERT_RP=WSRP_BASENS+"/InsertResourceProperties/InsertResourcePropertiesRequest";

	/**
	 * the wsa:Action for UpdateResourceProperties
	 */
	public static final String WSRP_UPDATE_RP=WSRP_BASENS+"/UpdateResourceProperties/UpdateResourcePropertiesRequest";

	/**
	 * the wsa:Action for DeleteResourceProperties
	 */
	public static final String WSRP_DELETE_RP=WSRP_BASENS+"/DeleteResourceProperties/DeleteResourcePropertiesRequest";

	public static final String RP="http://docs.oasis-open.org/wsrf/rp-2";

	/**
	 * the wsa:Action for DeleteChildResources
	 */
	public static final String WSRP_DELETE_CHILDREN=WSRP_BASENS+"/DeleteChildResources/DeleteChildResourcesRequest";

	@WebMethod(action=WSRP_DELETE_RP)
	@WebResult(targetNamespace=RP, name="DeleteResourcePropertiesResponse")
	public DeleteResourcePropertiesResponseDocument DeleteResourceProperties(
			@WebParam(targetNamespace=RP, name="DeleteResourceProperties")
			DeleteResourcePropertiesDocument in) 
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_PUT_RP_DOCUMENT)
	@WebResult(targetNamespace=RP, name="PutResourcePropertyDocumentResponse")
	public PutResourcePropertyDocumentResponseDocument PutResourcePropertyDocument(
			@WebParam(targetNamespace=RP, name="PutResourcePropertyDocument")
			PutResourcePropertyDocumentDocument1 in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_INSERT_RP)
	@WebResult(targetNamespace=RP, name="InsertResourcePropertiesResponse")
	public InsertResourcePropertiesResponseDocument InsertResourceProperties(
			@WebParam(targetNamespace=RP, name="InsertResourcePropertiesDocument")
			InsertResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_GET_RP_DOCUMENT)
	@WebResult(targetNamespace=RP, name="GetResourcePropertyDocumentResponse")
	public GetResourcePropertyDocumentResponseDocument GetResourcePropertyDocument(
			@WebParam(targetNamespace=RP, name="GetResourcePropertyDocument")
			GetResourcePropertyDocumentDocument1 in)
					throws BaseFault,ResourceUnknownFault, ResourceUnavailableFault;

	@WebMethod(action=WSRP_UPDATE_RP)
	@WebResult(targetNamespace=RP, name="UpdateResourcePropertiesResponse")
	public UpdateResourcePropertiesResponseDocument UpdateResourceProperties(
			@WebParam(targetNamespace=RP, name="UpdateResourceProperties")
			UpdateResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_GET_RP) 
	@WebResult(targetNamespace=RP, name="GetResourcePropertyResponse")
	public GetResourcePropertyResponseDocument GetResourceProperty(
			@WebParam(targetNamespace=RP, name="GetResourceProperty")
			GetResourcePropertyDocument in)
					throws BaseFault,ResourceUnknownFault, ResourceUnavailableFault,InvalidResourcePropertyQNameFault;

	@WebMethod(action=WSRP_GET_MULTIPLE_RP)
	@WebResult(targetNamespace=RP, name="GetMultipleResourcePropertiesResponse")
	public GetMultipleResourcePropertiesResponseDocument GetMultipleResourceProperties(
			@WebParam(targetNamespace=RP, name="GetMultipleResourceProperties")
			GetMultipleResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_QUERY_RP)
	@WebResult(targetNamespace=RP, name="QueryResourcePropertiesResponse")
	public QueryResourcePropertiesResponseDocument QueryResourceProperties(
			@WebParam(targetNamespace=RP, name="QueryResourceProperties")
			QueryResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	@WebMethod(action=WSRP_SET_RP)
	@WebResult(targetNamespace=RP, name="SetResourcePropertiesResponse")
	public SetResourcePropertiesResponseDocument SetResourceProperties(
			@WebParam(targetNamespace=RP, name="SetResourceProperties")
			SetResourcePropertiesDocument in)
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;

	// custom UNICORE extension: delete a bunch of "child" resources
	@WebMethod(action=WSRP_DELETE_CHILDREN)
	@WebResult(targetNamespace=RP, name="DeleteChildResourcesResponse")
	public DeleteChildResourcesResponseDocument DeleteChildResources(
			@WebParam(targetNamespace=RP, name="DeleteChildResources")
			DeleteChildResourcesDocument in) 
					throws ResourceUnknownFault, ResourceUnavailableFault, BaseFault;
}