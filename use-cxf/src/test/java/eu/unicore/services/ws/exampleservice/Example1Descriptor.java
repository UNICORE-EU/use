package eu.unicore.services.ws.exampleservice;

import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.ws.WSServerResource;
import eu.unicore.services.ws.cxf.CXFService;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;

public class Example1Descriptor extends DeploymentDescriptorImpl {

	public Example1Descriptor() {
		this.name = "example";
		this.type = CXFService.TYPE;
		this.interfaceClass = WSServerResource.class;
		this.implementationClass = WSResourceHomeImpl.class;
	}

}
