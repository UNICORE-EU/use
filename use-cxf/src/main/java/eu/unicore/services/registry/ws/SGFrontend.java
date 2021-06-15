package eu.unicore.services.registry.ws;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.GDateBuilder;
import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlDateTime;
import org.apache.xmlbeans.XmlDuration;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.sg2.AddDocument;
import org.oasisOpen.docs.wsrf.sg2.AddResponseDocument;
import org.oasisOpen.docs.wsrf.sg2.ServiceGroupRPDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.registry.ServiceRegistryImpl;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.exceptions.ResourceNotDestroyedFault;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;
import eu.unicore.services.ws.impl.WSRFFrontend;
import eu.unicore.services.ws.sg.ServiceGroupEntry;
import eu.unicore.services.ws.sg.ServiceGroupRegistration;
import eu.unicore.util.Log;

public class SGFrontend extends WSRFFrontend implements ServiceGroupRegistration {

	public static final String sre_uuid = "__sge_uuid";

	ServiceRegistryImpl resource;

	public SGFrontend(ServiceRegistryImpl r) {
		super(r, ServiceGroupRPDocument.type.getDocumentElementName(), null);
		this.resource = r;
		addRenderer(new SGEntryRenderer(resource));
	}

	@Override
	public AddResponseDocument Add(AddDocument in) throws BaseFault {
		try{
			EndpointReferenceType memberEPR = in.getAdd().getMemberEPR();
			String endpoint = memberEPR.getAddress().getStringValue();
			Calendar requestedTT =  makeCalendar(in.getAdd().getInitialTerminationTime());
			Map<String,String>content = parse(memberEPR);
			String entryID = resource.addEntry(endpoint, content, requestedTT);
			Calendar tt = requestedTT!=null? requestedTT : resource.getDefaultEntryLifetime();
			AddResponseDocument res = AddResponseDocument.Factory.newInstance();
			res.addNewAddResponse().setTerminationTime(tt);
			String url = kernel.getContainerProperties().getBaseUrl()+ServiceGroupEntry.SERVICENAME+"?res="+entryID;
			res.getAddResponse().addNewServiceGroupEntryReference().addNewAddress().setStringValue(url);
			return res;
		}
		catch(Exception ex){
			Log.logException("Error adding", ex, logger);
			throw BaseFault.createFault(ex.getMessage());
		}
	}


	@Override
	public DestroyResponseDocument Destroy(DestroyDocument in) throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		throw ResourceNotDestroyedFault.createFault("Not destroyed."); 
	}

	/**
	 * make a Calendar instance from the initial TT as supplied to Add()<br>
	 * the SG spec says this is either xsd:dateTime or xsd:duration
	 * @param initialTT
	 * @return Calendar
	 */
	public Calendar makeCalendar(Object initialTT){
		if(initialTT==null)return null;
		Calendar c=null;
		try{
			XmlDateTime d=XmlDateTime.Factory.newValue(initialTT);
			c=d.getCalendarValue();
			return c;
		}
		catch(Exception e){}

		try{
			XmlDuration d=XmlDuration.Factory.newValue(initialTT);
			GDuration duration=d.getGDurationValue();
			GDateBuilder b=new GDateBuilder();
			b.addGDuration(duration);
			c=Calendar.getInstance();
			c.setTime(b.getDate());
			return c;
		}catch(Exception e){}
		return null;
	}

	@Override
	public XmlObject getResourcePropertyResponseDocument() {
		try{
			//this is a workaround for a very weird behaviour when doing xpath queries
			return XmlObject.Factory.parse(super.getResourcePropertyResponseDocument().toString());
		}catch(Exception e){
			Log.logException("", e,logger);
			return null;
		}
	}
	
	public static Map<String,String> parse(EndpointReferenceType memberEPR){
		Map<String,String> res = new HashMap<>();
		String dn = WSUtilities.extractServerIDFromEPR(memberEPR);
		if(dn!=null){
			res.put(RegistryClient.SERVER_IDENTITY,dn);
		}
		QName q = WSUtilities.extractInterfaceName(memberEPR);
		if(q!=null){
			res.put(RegistryClient.INTERFACE_NAME,q.getLocalPart());
			if(q.getNamespaceURI()!=null)res.put(RegistryClient.INTERFACE_NAMESPACE,q.getNamespaceURI());
		}
		res.put(RegistryClient.ENDPOINT, memberEPR.getAddress().getStringValue());
		String pem = WSUtilities.extractPublicKey(memberEPR);
		if(pem!=null){
			res.put(RegistryClient.SERVER_PUBKEY,pem);
		}
		return res;
	}

}
