package eu.unicore.services.rest.impl;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.codahale.metrics.Meter;

import eu.unicore.security.Client;
import eu.unicore.security.OperationType;
import eu.unicore.security.SEIOperationType;
import eu.unicore.security.SecurityException;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.wsutil.CXFUtils;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.Model;
import eu.unicore.services.Resource;
import eu.unicore.services.admin.ResourceAvailability;
import eu.unicore.services.exceptions.ResourceUnavailableException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.impl.SecuredResourceImpl;
import eu.unicore.services.impl.SecuredResourceModel;
import eu.unicore.services.persistence.PersistenceSettings;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.security.SecurityManager;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.security.util.ResourceDescriptor;
import eu.unicore.services.utils.TimeProfile;
import eu.unicore.util.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public final class USERestInvoker extends JAXRSInvoker {

	private static final Logger logger = Log.getLogger(Log.SERVICES, USERestInvoker.class);

	private final Kernel kernel;

	private final SecurityManager securityManager;

	private static Meter callFrequency; 

	static final String LOCKEDKEY = USERestInvoker.class.getName()+"LOCKED";
	static final String HOME = USERestInvoker.class.getName()+"HOME";
	static final String HAVEMESSAGESKEY = USERestInvoker.class.getName()+"HAVEMESSAGES";

	public USERestInvoker(Kernel kernel){
		super();
		this.kernel = kernel;
		this.securityManager = kernel.getSecurityManager();
		setupMetrics();
	}

	private synchronized void setupMetrics(){
		if(callFrequency == null){
			callFrequency = (Meter)kernel.getMetrics().get("use.throughput");
		}
	}
	
	@Override
	public Object invoke(Exchange exchange, Object request){
		ThreadContext.clearAll();
		if(!kernel.isAvailable()) {
			int i=0;
			while(!kernel.isAvailable() && i<30) {
				i++;
				try{
					Thread.sleep(1000);
				}catch(InterruptedException ie) {}
			}
		}
		if(!kernel.isAvailable()){
			throw new ResourceUnavailableException("Service container is <" + kernel.getState() + ">, not (yet) operational.");
		}
		TimeProfile tp = AuthZAttributeStore.getTimeProfile();
		Object res = super.invoke(exchange, request);
		if(tp.isEnabled()) {
			tp.time("exit");
			tp.log();
		}
		return res;
	}
	
	@Override
	public Object getServiceObject(Exchange exchange) {
		Object o = super.getServiceObject(exchange);
		if(o instanceof KernelInjectable){
			((KernelInjectable)o).setKernel(kernel);
		}
		configureServiceObject(o, exchange);
		return o;
	}
	
	private void configureServiceObject(Object o, Exchange exchange) {
		String serviceName = (String)exchange.getService().get(RestService.SIMPLE_SERVICE_NAME);
		String homeName = extractHome(exchange); 
		String resourceID = extractUniqueID(exchange);
		Home home =  kernel.getHome(homeName);
		boolean hasMessages = false;
		boolean needLock = false;
		Resource r = null;
		
		final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
		String action = ori.getHttpMethod();
		Method method = ori.getMethodToInvoke();

		// is this a sub-resource resolution? 
		// TODO is there a safer way in CXF to decide this?
		boolean isSubresource = action == null;

		OperationType opType = null;
		if("GET".equals(action)) {
			opType = OperationType.read;
		}
		else if ("POST".equals(action)) {
			opType = OperationType.write;
		}
		else {
			opType = OperationType.modify;
		}
		SEIOperationType opAnnotation = method.getAnnotation(SEIOperationType.class);
		if(opAnnotation!=null){
			opType = opAnnotation.value();
		}

		if(o instanceof RESTRendererBase){
			RESTRendererBase rrb = (RESTRendererBase)o;
			rrb.setKernel(kernel);
			rrb.setBaseURL(getBaseURL(exchange, serviceName));
		}

		if(o instanceof BaseRESTController){
			BaseRESTController br = (BaseRESTController)o;
			br.setHome(home);
			PersistenceSettings ps = PersistenceSettings.get(br.getClass());
			try{
				hasMessages = resourceID!=null && br.usesKernelMessaging()
						&& kernel.getMessaging().hasMessages(resourceID);
				if(hasMessages)exchange.put(HAVEMESSAGESKEY, Boolean.TRUE);

				// we must lock if we have internal updates or the service method cannot be run concurrently
				needLock = hasMessages || !(isSubresource || ps.isConcurrentMethod(method.getName()));

				// get and inject the resource and the model
				if(home!=null && resourceID!=null){
					if(ResourceAvailability.isUnavailable(resourceID)){
						throw new ResourceUnavailableException("Resource <"+resourceID
								+"> set to unavailable by an administrator.");
					}
					logger.debug("Invoking on resource ID : {}", resourceID);
					r = needLock ? home.getForUpdate(resourceID) : home.get(resourceID);
					br.setModel(r.getModel());
					br.setResource(r);
					if(needLock) {
						exchange.put(LOCKEDKEY, r);
						exchange.put(HOME, home);
					}
				}
			}catch(ResourceUnavailableException pe){
				Response resp = RESTRendererBase.handleError(503, "Cannot access requested resource", pe, logger);
				throw new WebApplicationException(resp);
			}
			catch(ResourceUnknownException rue){
				Response resp = Response.status(Status.NOT_FOUND).build();
				throw new WebApplicationException(resp);
			}
			catch(Exception e) {
				Response resp = RESTRendererBase.handleError(500, "Cannot access requested resource", e, logger);
				throw new WebApplicationException(resp);
			}
		}
		if(!isSubresource){
			if(r instanceof SecuredResourceImpl) {
				try{
					((SecuredResourceImpl)r).updateSecurityTokensBeforeAIP(AuthZAttributeStore.getTokens());
				}catch(Exception e){}
			}
			createClient();
			accessControl(serviceName, home, resourceID, action, opType, r, exchange);
		}
		if(r!=null){
			r.activate();
		}
		callFrequency.mark();
	}

	private void createClient() {
		SecurityTokens tokens = AuthZAttributeStore.getTokens();
		if(tokens!=null) {
			Client client = kernel.getSecurityManager().createClientWithAttributes(tokens);
			if(client!=null && client.getSecurityTokens()!=null){
				kernel.getSecurityManager().collectDynamicAttributes(client);
				AuthZAttributeStore.setClient(client);
			}
		}
	}

	private void accessControl(String serviceName, Home home, String resourceID, String action, OperationType opType, Resource r, Exchange exchange)
	throws WebApplicationException {
		String accessControlOnService = serviceName;
		if(home!=null && resourceID!=null){
			accessControlOnService = home.getServiceName();
		}
		try{
			checkAccess(action, opType, accessControlOnService, r);
		}catch(SecurityException ex){
			// set a proper JAX-RS response for CXF to pick up
			Response resp = Response.status(Status.FORBIDDEN).build();
			throw new WebApplicationException(resp);
		}

	}

	private String extractUniqueID(Exchange exchange){
		String idParamName = "uniqueID";
		MultivaluedMap<String, String> values = getTemplateValues(exchange.getInMessage());
		List<String> ids = values.get(idParamName);
		return ids!=null && ids.size()>0 ? ids.get(0): null;
	}

	/**
	 * Figure out the name of the Home to use.
	 * The order is
	 * <ul>
	 * <li>a method parameter "home"</li>
	 * <li>the home parameter of the {@link USEResource} annotation (if it exists)</li>
	 * <li>the REST service name as fall-back</li>
	 * </ul>
	 * 
	 * @param exchange
	 * @return the name of the Home, or null if not applicable
	 */
	private String extractHome(Exchange exchange){
		String idParamName = "home";
		MultivaluedMap<String, String> values = getTemplateValues(exchange.getInMessage());
		List<String> ids = values.get(idParamName);
		String name = ids!=null && ids.size()>0 ? ids.get(0): null;

		if(name==null){
			name = (String)exchange.getService().get(RestService.SIMPLE_SERVICE_NAME);
			final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
			final ClassResourceInfo cri = ori.getClassResourceInfo();
			if(cri.getResourceClass()!=null && cri.getResourceClass().getAnnotation(USEResource.class)!=null){
				name = cri.getResourceClass().getAnnotation(USEResource.class).home();
			}
		}
		return name; 
	}


	/**
	 * check access control
	 * 
	 * @see SecurityManager#checkAuthorisation(Client, ActionDescriptor, ResourceDescriptor)
	 * 
	 * @param action
	 * @param serviceName
	 * @param r - resource (which may be null)
	 */
	private void checkAccess(String action, OperationType opType, String serviceName, Resource r){
		Client c = AuthZAttributeStore.getClient();
		Model model = r!=null? r.getModel() : null; 
		String uniqueID = null;
		String owner = null;
		try{
			owner = kernel.getContainerSecurityConfiguration().getCredential().getSubjectName();
		}catch(Exception ex){}
		boolean aclCheckPassed = false;
		if(model!=null && model instanceof SecuredResourceModel){
			SecuredResourceModel secure = (SecuredResourceModel)model;
			uniqueID=model.getUniqueID();
			owner = secure.getOwnerDN();
			// user ACL
			aclCheckPassed = kernel.getSecurityManager().checkAcl(secure.getAcl(),opType,c);
		}
		ResourceDescriptor rd = new ResourceDescriptor(serviceName, uniqueID, owner);
		rd.setAclCheckOK(aclCheckPassed);
		securityManager.checkAuthorisation(c, new ActionDescriptor(action, opType), rd);
	}

	private String getBaseURL(Exchange exchange, String serviceName){
		StringBuilder base = new StringBuilder();
		String gwBase = CXFUtils.getServletRequest(exchange.getInMessage()).getHeader(AuthNHandler.GW_EXTERNAL_URL);
		if(gwBase != null){
			base.append(gwBase);
		}
		else{
			base.append(kernel.getContainerProperties().getContainerURL());
		}
		base.append("/rest/").append(serviceName);
		return base.toString();
	}
}
