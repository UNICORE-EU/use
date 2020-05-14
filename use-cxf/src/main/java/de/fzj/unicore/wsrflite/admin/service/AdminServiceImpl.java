/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.wsrflite.admin.service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;

import com.codahale.metrics.Metric;

import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.wsrflite.utils.MetricUtils;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.AdminActionValueType;
import de.fzj.unicore.wsrflite.xmlbeans.AdminService;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricsRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetMetricsResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesRequestDocument;
import de.fzj.unicore.wsrflite.xmlbeans.GetServiceInstancesResponseDocument;
import de.fzj.unicore.wsrflite.xmlbeans.MetricValueDocument.MetricValue;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.Log;

/**
 * @author j.daivandy@fz-juelich.de
 * @author schuller
 */
public class AdminServiceImpl extends WSResourceImpl implements AdminService {

	private static final Logger logger=Log.getLogger(Log.ADMIN, AdminServiceImpl.class);
	
	/**
	 * in this special case, we use the constructor to setup the service instance
	 */
	public AdminServiceImpl(Kernel kernel, Home home){
		this.kernel=kernel;
		this.home=home;
		frontendDelegate.setResourcePropertyDocumentQName(RPAdminServiceQName);
		addRenderer(new ServiceEntriesRenderer(this));
		addRenderer(new MonitorEntriesRenderer(this));
		addRenderer(new AdminActionsRenderer(this));
		try{
			super.initialise(new InitParameters());
		}catch(Exception  ex){
			throw new RuntimeException(ex);
		}
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return RPAdminServiceQName;
	}

	/**
	 * Retrieves a list of currently running instances of a WSRF Web Service
	 */	
	public GetServiceInstancesResponseDocument getServiceInstances(GetServiceInstancesRequestDocument req) throws BaseFault {
		try{
			String serviceName = req.getGetServiceInstancesRequest().getServiceName();		
			Collection<String> uids = home.getKernel().getPersistenceManager().getPersist(serviceName).getUniqueIDs();				
			GetServiceInstancesResponseDocument res = GetServiceInstancesResponseDocument.Factory.newInstance();
			res.addNewGetServiceInstancesResponse();
			res.getGetServiceInstancesResponse().setUidArray(uids.toArray(new String[uids.size()]));
			return res;
		}
		catch(Exception e) {
			Log.logException(e.getMessage(),e,logger);
			throw BaseFault.createFault("", e);
		}
	}


	/**
	 * Gets a list of values filtered using the given metric names
	 */
	public GetMetricsResponseDocument getMetrics(GetMetricsRequestDocument req) {
		String[] mNames=req.getGetMetricsRequest().getNameArray();
		boolean filterByName = mNames!=null && mNames.length>0;

		Collection<String>metricNames=filterByName?Arrays.asList(mNames):new ArrayList<String>();
		
		GetMetricsResponseDocument res = GetMetricsResponseDocument.Factory.newInstance();
		res.addNewGetMetricsResponse();
		Map<String,Metric> ms = kernel.getMetricRegistry().getMetrics();
		
		Map<String,Metric> metrics = new HashMap<>();
		
		for(Map.Entry<String, Metric> e: ms.entrySet()) {
			if(filterByName && !metricNames.contains(e.getKey())){
				continue;
			}
			else{
				metrics.put(e.getKey(), e.getValue());
			}
		}
		Calendar c = Calendar.getInstance();
		Map<String,String> values = MetricUtils.getValues(metrics);
		for(Map.Entry<String, String> val: values.entrySet()) {
			MetricValue mv = res.getGetMetricsResponse().addNewMetricValue();
			mv.setTimestamp(c);
			mv.setName(val.getKey());
			mv.setValue(val.getValue());
		}
		return res;		
	}

	public GetMetricResponseDocument getMetric(GetMetricRequestDocument req) {
		String name = req.getGetMetricRequest().getName();
		GetMetricResponseDocument res = GetMetricResponseDocument.Factory.newInstance();
		res.addNewGetMetricResponse();
		Metric metric = kernel.getMetricRegistry().getMetrics().get(name);
		if(metric!=null){
			MetricValue mv = res.getGetMetricResponse().addNewMetricValue();
			mv.setTimestamp(Calendar.getInstance());
			mv.setName(name);
			mv.setValue(MetricUtils.getValue(metric));
		}
		return res;		
	}

	
	@Override
	public AdminActionResponseDocument invokeAdminAction(AdminActionRequestDocument req)throws BaseFault {
		String name=req.getAdminActionRequest().getName();
		AdminAction action=home.getKernel().getAdminActions().get(name);
		if(action==null){
			throw BaseFault.createFault("No admin action named <"+name+">");
		}
		Map<String,String>params=new HashMap<String, String>();
		for(AdminActionValueType r: req.getAdminActionRequest().getParameterArray()){
			params.put(r.getName(), r.getValue());
		}
		logger.info("Invoking administrative action <"+name+"> :"
				+" client='"+getClient().getDistinguishedName()+"'"
				+" role="+getClient().getRole().getName() + " parameters="+params);
		
		AdminActionResult result=action.invoke(params, kernel);
		
		logger.info("Administrative action <"+name+"> success="+result.successful()
				+" message='"+result.getMessage()+"' results="+result.getResults()
				+" resultReferences="+result.getResultReferences());
		
		AdminActionResponseDocument response=AdminActionResponseDocument.Factory.newInstance();
		response.addNewAdminActionResponse();
		response.getAdminActionResponse().setSuccess(result.successful());
		response.getAdminActionResponse().setMessage(result.getMessage());
		for(Map.Entry<String, String> e: result.getResults().entrySet()){
			AdminActionValueType r=response.getAdminActionResponse().addNewResults();
			r.setName(e.getKey());
			r.setValue(e.getValue());
		}
		return response;
	}

}
