/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
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

package eu.unicore.services.security.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.NDC;

import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Base for AIPs implementation that combines the results from a chain of attribute sources using
 * a configurable combining policy:
 * <ul>
 *  <li>FIRST_APPLICABLE: the first source returning any result is used</li>
 *  <li>FIRST_ACCESSIBLE: the first accessible (i.e. not throwing an exception) source is used</li>
 *  <li>MERGE_LAST_OVERRIDES (default): all results are combined, so that the later 
 *      attribute sources in the chain can override earlier ones</li>
 *  <li>MERGE : all results are combined, and valid attribute values of the same attribute are merged.
 *  Note that in case of default values for incarnation attributes (used if user doesn't request
 *  a particular value) merging is not done, but values are overridden. This is as for those
 *  attributes in nearly all cases multiple values doesn't make sense (user can have one uid,
 *  primary gid, job may be submitted only to one queue).</li>  
 * </ul> 
 * <p>
 * For each incarnation attribute a final value is computed as follow:
 * <ul>
 *  <li> use the value provided by user (if it is valid, if not - fail the request). If user didn't provide a value </li>
 *  <li> use the value from the preferred VO if present, if not </li>
 *  <li> use the default attribute value </li>
 * </ul>
 * @author schuller
 * @author golbi
 */
public abstract class BaseAttributeSourcesChain<T extends IAttributeSourceBase> implements IAttributeSourceBase {

	protected List<T> chain;
	protected List<String> names;
	protected String name;
	protected String orderString;
	protected String combinerName;
	protected CombiningPolicy combiner;
	protected Properties properties = null;
	protected boolean started = false;
	
	/**
	 * will configure all the aips in the chain
	 */
	@Override
	public void configure(String name) throws ConfigurationException {
		this.name = name;
		initOrder();
		for(int i=0; i<chain.size(); i++){
			NDC.push(names.get(i));
			try {
				chain.get(i).configure(names.get(i));
			} finally {
				NDC.pop();
			}
		}
		initCombiningPolicy();
	}
	
	/**
	 * starts all aips in the chain
	 */
	@Override
	public void start(Kernel kernel) throws Exception {
		for(int i=0; i<chain.size(); i++){
			NDC.push(names.get(i));
			try {
				T aip = chain.get(i);
				if(aip instanceof ExternalSystemConnector){
					kernel.getExternalSystemConnectors().add((ExternalSystemConnector)aip);
				}
				aip.start(kernel);
			} finally {
				NDC.pop();
			}
		}
		started = true;
	}
	
	@Override
	public String getStatusDescription() {
		assert started : "This object must be started before use.";
		StringBuilder sb=new StringBuilder();
		String newline = System.getProperty("line.separator");
		if(chain.size()==0){
			sb.append("N/A");
		}
		if(chain.size()>1){
			sb.append(" Combining policy: ").append(String.valueOf(combiner));
		}
		for(T a: chain){
			sb.append(newline );
			sb.append(" * ");
			sb.append(a.getStatusDescription());
		}
		return sb.toString();
	}
	
	public List<T> getChain(){
		assert started : "This object must be started before use.";
		return Collections.unmodifiableList(chain);
	}
	
	public CombiningPolicy getCombiningPolicy(){
		assert started : "This object must be started before use.";
		return combiner;
	}
	
	/**
	 * merge info from "slave" map into master map, overriding info already present
	 */
	void merge(Map<String,String[]>master, Map<String,String[]>slave){
		for(Map.Entry<String,String[]>e: slave.entrySet()){
			master.put(e.getKey(),e.getValue());
		}
	}

	public void setProperties(Properties p) {
		properties = p;
	}

	public void setOrder(String order) {
		orderString = order;
	}

	public void setCombiningPolicy(String name) {
		combinerName = name;
	}
	
	protected abstract void initOrder() throws ConfigurationException;
	
	private void initCombiningPolicy() throws ConfigurationException {
		if(MergeLastOverrides.NAME.equalsIgnoreCase(combinerName)){
			combiner=new BaseAttributeSourcesChain.MergeLastOverrides();
		}
		else if(Merge.NAME.equalsIgnoreCase(combinerName)){
			combiner=new BaseAttributeSourcesChain.Merge();
		}
		else if(FirstApplicable.NAME.equalsIgnoreCase(combinerName)){
			combiner=new BaseAttributeSourcesChain.FirstApplicable();
		}
		else if(FirstAccessible.NAME.equalsIgnoreCase(combinerName)){
			combiner=new BaseAttributeSourcesChain.FirstAccessible();
		}
		else{
			try{
				Object c=Class.forName(combinerName).getConstructor().newInstance();
				combiner=(CombiningPolicy)c;
			}
			catch(Exception ex){
				throw new ConfigurationException("Can't create combining policy <"+combinerName+">");
			}
		}
	}

	
	/**
	 * defines how attributes should be combined
	 */
	public static interface CombiningPolicy{
		
		/**
		 * combines new attributes with the already existing ones
		 * @param master - the already existing attributes
		 * @param newAttributes - the new attributes
		 * @return true if next attribute sources should be evaluated, false if processing should be stopped.
		 */
		public boolean combineAttributes(SubjectAttributesHolder master, 
				SubjectAttributesHolder newAttributes);
		
	}
	
	/**
	 * first applicable: only the first not empty map of attributes is used
	 */
	public static class FirstApplicable implements CombiningPolicy {
		public static final String NAME = "FIRST_APPLICABLE";
		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			//shouldn't happen but check anyway
			if (master.isPresent()) {
				return false;
			} else {
				if (!newAttributes.isPresent())
					return true;
				master.addAllOverwritting(newAttributes);
				return false;
			}
		}
		
		public String toString(){
			return NAME;
		}
	}

	/**
	 * first accessible: the answer from the first accessible attribute source is used. It is
	 * assumed that AttributeSource throws an exception when there is communication error.
	 */
	public static class FirstAccessible implements CombiningPolicy{
		public static final String NAME = "FIRST_ACCESSIBLE"; 
		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			//shouldn't happen but check anyway
			if(master.isPresent()) {
				return false;
			} else {
				master.addAllOverwritting(newAttributes);	
				return false;
			}
		}
		
		public String toString(){
			return NAME;
		}
	}

	
	/**
	 * merge_last_overrides:  new attributes overwrite existing ones
	 */
	public static class MergeLastOverrides implements CombiningPolicy{
		public static final String NAME = "MERGE_LAST_OVERRIDES"; 
		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			master.addAllOverwritting(newAttributes);
			return true;
		}
		
		public String toString(){
			return NAME;
		}
	}
	
	/**
	 * Merge:  attributes with the same key are combined (values are added).
	 * This is always OK for XACML attributes (only duplicates are not maintained) and
	 * for lists of valid values. However for regular incarnation attributes 
	 * (like xlogin, role, primary gid, queue) only one value make sense. So in case of 
	 * default values obtained from AS of incarnation attributes the same policy is 
	 * used as in MERGE_LAST_OVERRIDS. 
	 */
	public static class Merge implements CombiningPolicy{
		public static final String NAME = "MERGE"; 
			
		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			master.addAllMerging(newAttributes);
			return true;
		}
		
		public String toString(){
			return NAME;
		}
	}

	@Override
	public String getName()
	{
		return "User mapping & user attributes";
	}
}
