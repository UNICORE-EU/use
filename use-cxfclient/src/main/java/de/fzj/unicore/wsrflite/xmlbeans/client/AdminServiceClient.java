package de.fzj.unicore.wsrflite.xmlbeans.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.AdminActionRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionValueType;
import de.fzj.unicore.wsrflite.xmlbeans.AdminService;
import de.fzj.unicore.wsrflite.xmlbeans.AdminServicePropertiesDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricsRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricsResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.MetricValueDocument.MetricValue;
import de.fzj.unicore.wsrflite.xmlbeans.ServiceEntryDocument.ServiceEntry;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * client for the AdminService
 * 
 * @author j.daivandy@fz-juelich.de
 * @author schuller
 */
public class AdminServiceClient extends BaseWSRFClient {

	private final AdminService admin;

	public AdminServiceClient(EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		this(epr.getAddress().getStringValue(),epr,sec);
	}

	public AdminServiceClient(String endpointUrl, EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		super(endpointUrl, epr, sec);
		admin = makeProxy(AdminService.class);
	}

	public AdminServicePropertiesDocument getResourcePropertiesDocument()throws Exception{
		return AdminServicePropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}

	public ServiceEntry[] getServiceNames() throws Exception{
		ServiceEntry[] entries=getResourcePropertiesDocument().getAdminServiceProperties().getServiceEntryArray();
		return entries;	
	}	
	
	/**
	 * get a single metric
	 * @param metricName
	 * @throws Exception
	 */
	public MetricValue getMetric(String metricName) throws Exception{
		GetMetricRequestDocument req = GetMetricRequestDocument.Factory.newInstance();
		req.addNewGetMetricRequest().setName(metricName);
		GetMetricResponseDocument res = admin.getMetric(req);
		return res.getGetMetricResponse().getMetricValue();
	}
	
	
	/**
	 * get the metrics having the given names
	 * @param metricNames - list of metric names to retrieve (can be null or empty)
	 */
	public MetricValue[] getMetrics(String[] metricNames) throws Exception{		
		GetMetricsRequestDocument req = GetMetricsRequestDocument.Factory.newInstance();
		req.addNewGetMetricsRequest().setNameArray(metricNames);
		GetMetricsResponseDocument res = admin.getMetrics(req);
		return res.getGetMetricsResponse().getMetricValueArray();		
	}

	public GetServiceInstancesResponseDocument getServiceInstances(String serviceName) throws Exception {
		GetServiceInstancesRequestDocument req = GetServiceInstancesRequestDocument.Factory.newInstance();
		req.addNewGetServiceInstancesRequest().setServiceName(serviceName);
		return admin.getServiceInstances(req);		
	}

	public List<AdminServiceClient.AdminAction>getAdminActions()throws Exception{
		List<AdminServiceClient.AdminAction>result=new ArrayList<AdminServiceClient.AdminAction>();
		for(de.fzj.unicore.wsrflite.xmlbeans.AdminActionDocument.AdminAction a : 
			getResourcePropertiesDocument().getAdminServiceProperties().getAdminActionArray()){
			result.add(new AdminAction(a.getName(),a.getDescription()));
		}
		//add old style actions as well
		String[]adminActions=getResourcePropertiesDocument().getAdminServiceProperties().getAvailableAdminActionArray();
		if(adminActions!=null){
			for(String s: adminActions){
				result.add(new AdminAction(s,"n/a"));
			}
		}
		return result;
	}

	/**
	 * invoke the named admin action
	 * @param name - the name of the action to invoke
	 * @param params - parameters
	 * @return {@link AdminActionResult} containing the service's response
	 * @throws Exception
	 */
	public AdminActionResult invokeAdminAction(String name, Map<String,String>params) throws Exception{
		AdminActionRequestDocument aard=AdminActionRequestDocument.Factory.newInstance();
		aard.addNewAdminActionRequest().setName(name);
		if(params!=null){
			for(Map.Entry<String, String>e: params.entrySet()){
				AdminActionValueType p=aard.getAdminActionRequest().addNewParameter();
				p.setName(e.getKey());
				p.setValue(e.getValue());
			}
		}
		AdminActionResponseDocument response=admin.invokeAdminAction(aard);
		String message=response.getAdminActionResponse().getMessage();
		AdminActionValueType[] results=response.getAdminActionResponse().getResultsArray();
		boolean success=response.getAdminActionResponse().getSuccess();
		AdminActionResult result=new AdminActionResult(success, message);
		if(results!=null){
			for(AdminActionValueType r: results){
				result.addResult(r.getName(), r.getValue());
			}
		}
		return result;
	}
	
	/**
	 * holds results of an invocation of an admin action
	 */
	public static class AdminActionResult{
		
		private final Map<String,String>results=new HashMap<String,String>();
		private final boolean success;
		private final String message;
		
		public AdminActionResult(boolean success, String message){
			this.success=success;
			this.message=message;
		}
		
		public boolean successful(){
			return success;
		}
		
		public String getMessage(){
			return message;
		}
		
		public Map<String,String>getResults(){
			return results;
		}
		
		public void addResult(String name, String value){
			if(value==null)results.remove(name);
			results.put(name,value);
		}
	}
	
	public static class AdminAction {
		public final String name,description;
		public AdminAction(String name, String description){
			this.name=name;
			this.description=description;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AdminAction other = (AdminAction) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
}
