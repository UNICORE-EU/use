package eu.unicore.services.ws;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

public class WSAOutHandler extends AbstractSoapInterceptor {
	
	private final EndpointReferenceType epr;
	
	public WSAOutHandler(EndpointReferenceType epr) {
		super(Phase.PRE_LOGICAL);
		this.epr=epr;
		getBefore().add(MAPAggregator.class.getName());
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		//set the WS-A parameters to the ones given in our EPR 
		AddressingProperties maps = new AddressingProperties();
		maps.setTo(WSUtilities.toCXF(epr));
		message.put(CLIENT_ADDRESSING_PROPERTIES, maps);
	}

}
