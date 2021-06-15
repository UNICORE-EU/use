package eu.unicore.services.registry.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsrf.sg2.EntryDocument;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import eu.unicore.services.Resource;
import eu.unicore.services.registry.ServiceRegistryModel;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * Renders the Entry fields of a service group RP document
 * 
 * 
 * @author schuller
 */
public class SGEntryRenderer extends ValueRenderer {

	public SGEntryRenderer(Resource parent){
		super(parent,EntryDocument.type.getDocumentElementName());
	}
	
	@Override
	protected Object getValue()throws NoSuchFieldException, IllegalAccessException{
		List<Map<String,String>>res = new ArrayList<>();
		ServiceRegistryModel model = (ServiceRegistryModel)parent.getModel();
		for(Map.Entry<String, Map<String,String>> e: model.getContents().entrySet()){
			Map<String,String> entry = new HashMap<>();
			entry.put(RegistryClient.ENDPOINT, e.getKey());
			entry.put(SGFrontend.sre_uuid, model.getEntryID(e.getKey()));
			entry.putAll(e.getValue());
			res.add(entry);
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	protected XmlObject renderValue(Object v){
		Map<String,String> value = (Map<String,String>)v;
		EntryDocument ed = EntryDocument.Factory.newInstance();
		EntryType entry = ed.addNewEntry();

		EndpointReferenceType sgeEpr = entry.addNewServiceGroupEntryEPR();
		String ep = parent.getKernel().getContainerProperties().getBaseUrl()+"/ServiceGroupEntry?res="
				+value.get(SGFrontend.sre_uuid);
		sgeEpr.addNewAddress().setStringValue(ep);

		entry.addNewContent();
		
		EndpointReferenceType memberEpr = entry.addNewMemberServiceEPR();
		memberEpr.addNewAddress().setStringValue(value.get(RegistryClient.ENDPOINT));
		String dn = value.get(RegistryClient.SERVER_IDENTITY);
		if(dn!=null)WSUtilities.addServerIdentity(memberEpr, dn);
		QName q = new QName(value.get(RegistryClient.INTERFACE_NAMESPACE), value.get(RegistryClient.INTERFACE_NAME));
		String pubkey = value.get(RegistryClient.SERVER_PUBKEY);
		if(pubkey!=null)WSUtilities.addServerPublicKey(memberEpr, pubkey);
		WSUtilities.addPortType(memberEpr, q);
		return ed;
	}
	

}
