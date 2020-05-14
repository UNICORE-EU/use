package de.fzj.unicore.wsrflite;

import javax.xml.namespace.QName;

/**
 * common constants, such as namespaces, porttypes and SOAP actions
 */
public interface WSRFConstants {


	public final String WSA_200508 = "http://www.w3.org/2005/08/addressing"; 
	
	//TODO add the w3c recommendation
	//String wsa200604 = "http://www.w3.org/2006/04/addressing";
	
	@Deprecated
	public final static QName FLE_RESOURCE_DISAMBIGUATOR=
		new QName("http://com.fujitsu.arcon.addressing",
				"ResourceDisambiguator");
	
	public final static QName U6_RESOURCE_ID=
		new QName("http://www.unicore.eu/unicore6",
				"ResourceId");
	
	
	public final static QName EPR_METADATA=
		new QName("http://www.w3.org/2005/08/addressing",
				"Metadata");
	
	public final static QName INTERFACE_NAME=
		new QName("http://www.w3.org/2005/08/addressing/metadata",
				"InterfaceName");

	/**
	 * QName of the EPR metadata field holding the server DN
	 */
	public final static QName SERVER_NAME=
		new QName("http://www.unicore.eu/unicore6",
				"ServerIdentity");
	/**
	 * QName of the EPR metadata field holding the "friendly name" of the EPR
	 */
	public final static QName PUBLIC_KEY =
			new QName("http://www.unicore.eu/unicore6",
					"ServerPublicKey");

	/**
	 * QName of the EPR metadata field holding the "friendly name" of the EPR
	 */
	public final static QName FRIENDLY_NAME=
			new QName("http://www.unicore.eu/unicore6",
					"FriendlyName");

	public static QName[] supported=new QName[]{
		U6_RESOURCE_ID,
		new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
		"Security"),
		//the next one is for backwards compatibility with older clients
		FLE_RESOURCE_DISAMBIGUATOR,
	};

	/**
	 * the base WS-RL (wsdl) namespace supported by this implementation
	 */
	public static final String WSRL_BASENS = "http://docs.oasis-open.org/wsrf/rlw-2";

	/**
	 * the wsa:Action for ImmediateResourceTermination
	 */
	public static final String WSRL_DESTROY = WSRL_BASENS
			+ "/ImmediateResourceTermination/DestroyRequest";

	/**
	 * the WSDL porttype for ImmediateResourceTermination
	 */
	public static final QName WSRL_DESTROY_PORT = new QName(WSRL_BASENS,"ImmediateResourceTermination");
	
	/**
	 * the wsa:Action for ScheduledResourceTermination
	 */
	public static final String WSRL_SCHEDULED = WSRL_BASENS
			+ "/ScheduledResourceTermination/SetTerminationTimeRequest";
	
	/**
	 * the WSDL porttype for ImmediateResourceTermination
	 */
	public static final QName WSRL_SET_TERMTIME_PORT = new QName (WSRL_BASENS,"ScheduledResourceTermination");
	
	/**
	 * the base WS-RP (wsdl) namespace supported by this implementation
	 */
	public static final String WSRP_BASENS="http://docs.oasis-open.org/wsrf/rpw-2";
	
	/**
	 * Query RP dialect
	 */
	public static final String QUERY_EXPRESSION_DIALECT_XPATH=
		"http://www.w3.org/TR/1999/REC-xpath-19991116";

	/**
	 * the wsa:Action for GetResourcePropertyDocument 
	 */
	public static final String WSRP_GET_RP_DOCUMENT=WSRP_BASENS+"/GetResourcePropertyDocument/GetResourcePropertyDocumentRequest";

	/**
	 * the WSDL porttype for GetResourcePropertyDocument
	 */
	public static final QName WSRP_GET_RP_DOCUMENT_PORT=new QName(WSRP_BASENS,"GetResourcePropertyDocument");

	/**
	 * the wsa:Action for GetResourceProperty
	 */
	public static final String WSRP_GET_RP=WSRP_BASENS+"/GetResourceProperty/GetResourcePropertyRequest";

	/**
	 * the porttype for GetResourceProperty
	 */
	public static final QName WSRP_GET_RP_PORT=new QName(WSRP_BASENS,"GetResourceProperty");

	/**
	 * the wsa:Action for GetMultipleResourceProperties
	 */
	public static final String WSRP_GET_MULTIPLE_RP=WSRP_BASENS+"/GetMultipleResourceProperties/GetMultipleResourcePropertiesRequest";
	
	/**
	 * the porttype for GetMultipleResourceProperties
	 */
	public static final QName WSRP_GET_MULTIPLE_RP_PORT=new QName(WSRP_BASENS,"GetMultipleResourceProperties");
	
	/**
	 * the wsa:Action for QueryResourceProperties
	 */
	public static final String WSRP_QUERY_RP=WSRP_BASENS+"/QueryResourceProperties/QueryResourcePropertiesRequest";
	
	/**
	 * the porttype for QueryResourceProperties
	 */
	public static final QName WSRP_QUERY_RP_PORT=new QName(WSRP_BASENS,"QueryResourceProperties");
	
	/**
	 * the QueryExpressionDialect ResourceProperty
	 */
	public static final QName WSRP_RP_QueryExpressionDialect=new QName(WSRP_BASENS,"QueryExpressionDialect");
	
	/**
	 * the wsa:Action for PutResourcePropertyDocument 
	 */
	public static final String WSRP_PUT_RP_DOCUMENT=WSRP_BASENS+"/PutResourcePropertyDocument/PutResourcePropertyDocumentRequest";
	/**
	 * the porttype for PutResourcePropertyDocument 
	 */
	public static final QName WSRP_PUT_RP_DOCUMENT_PORT=new QName(WSRP_BASENS,"PutResourcePropertyDocument");

	/**
	 * the wsa:Action for InsertResourceProperties
	 */
	public static final String WSRP_SET_RP=WSRP_BASENS+"/SetResourceProperties/SetResourcePropertiesRequest";
	
	/**
	 * the porttype for InsertResourceProperties
	 */
	public static final QName WSRP_SET_RP_PORT=new QName(WSRP_BASENS+"SetResourceProperties");
	
	/**
	 * the wsa:Action for InsertResourceProperties
	 */
	public static final String WSRP_INSERT_RP=WSRP_BASENS+"/InsertResourceProperties/InsertResourcePropertiesRequest";

	/**
	 * the porttype for InsertResourceProperties
	 */
	public static final QName WSRP_INSERT_RP_PORT=new QName(WSRP_BASENS,"InsertResourceProperties");

	/**
	 * the wsa:Action for UpdateResourceProperties
	 */
	public static final String WSRP_UPDATE_RP=WSRP_BASENS+"/UpdateResourceProperties/UpdateResourcePropertiesRequest";

	/**
	 * the porttype for UpdateResourceProperties
	 */
	public static final QName WSRP_UPDATE_RP_PORT=new QName(WSRP_BASENS,"UpdateResourceProperties");

	/**
	 * the wsa:Action for DeleteResourceProperties
	 */
	public static final String WSRP_DELETE_RP=WSRP_BASENS+"/DeleteResourceProperties/DeleteResourcePropertiesRequest";

	/**
	 * the porttype for DeleteResourceProperties
	 */
	public static final QName WSRP_DELETE_RP_PORT=new QName(WSRP_BASENS,"DeleteResourceProperties");

	/**
	 * the QName of the "termination time" resource property 
	 */
	public static final QName RPterminationTimeQName = 
		new QName("http://docs.oasis-open.org/wsrf/rl-2","TerminationTime");
		
	/**
	 * the QName of the "current time" resource property
	 */
	public static final QName RPcurrentTimeQName = 
		new QName("http://docs.oasis-open.org/wsrf/rl-2","CurrentTime");	
}
