package de.fzj.unicore.wsrflite.admin.service;

import java.math.BigInteger;
import java.util.Collection;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.Service;
import de.fzj.unicore.wsrflite.xmlbeans.ServiceEntryDocument;
import de.fzj.unicore.wsrflite.xmlbeans.ServiceEntryDocument.ServiceEntry;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class ServiceEntriesRenderer extends ValueRenderer {

	public ServiceEntriesRenderer(Resource parent){
		super(parent, ServiceEntryDocument.type.getDocumentElementName());
	}
	
	@Override
	protected ServiceEntryDocument[] getValue(){		
		Collection<Service> services = parent.getKernel().getServices();
		
		ServiceEntryDocument[]res=new ServiceEntryDocument[services.size()];
		int i=0;
		for(Service s: services){
			res[i]=ServiceEntryDocument.Factory.newInstance();
			ServiceEntry e=res[i].addNewServiceEntry();
			e.setServiceName(s.getName());
			
			Home h = s.getHome();			
			if(h!=null) {
				e.setIsWSRF(true);
				try{
					e.setNumberOfInstances(BigInteger.valueOf(h.getNumberOfInstances()));
				}catch(PersistenceException pe){
					throw new RuntimeException("Internal server error.", pe);
				}
			}
			else e.setIsWSRF(false);
			
			i++;
		}
		return res;
	}

}
