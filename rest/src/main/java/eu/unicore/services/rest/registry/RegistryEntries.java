package eu.unicore.services.rest.registry;

import java.util.Map;

import eu.unicore.services.registry.RegistryEntryModel;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import jakarta.ws.rs.Path;

/**
 * @author schuller
 */
@Path("/")
@USEResource(home="ServiceGroupEntry")
public class RegistryEntries extends ServicesBase {

	@Override
	protected String getResourcesName() {
		return "registryentries";
	}

	@Override
	protected String getPathComponent() {
		return "";
	}

	@Override
	public RegistryEntryModel getModel(){
		return (RegistryEntryModel)model;
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> status = super.getProperties();
		RegistryEntryModel m  = getModel();
		status.put("parent", m.getParentUID());
		return status;
	}

}
