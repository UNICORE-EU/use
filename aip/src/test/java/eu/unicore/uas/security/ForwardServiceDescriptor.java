package eu.unicore.uas.security;

import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.ws.cxf.CXFService;

public class ForwardServiceDescriptor extends DeploymentDescriptorImpl {

	public ForwardServiceDescriptor(){
		this.name = "TestService";
		this.type = CXFService.TYPE;
		this.implementationClass = ForwardServiceHome.class;
		this.interfaceClass = Forward.class;
	}
	
}
