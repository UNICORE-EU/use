package eu.unicore.services.security.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.services.ISubSystem;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.ContainerSecurityProperties;
import eu.unicore.services.security.IAttributeSourceBase;
import eu.unicore.util.Pair;
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
public abstract class BaseAttributeSourcesChain<T extends IAttributeSourceBase> 
implements IAttributeSourceBase, ISubSystem {

	protected List<T> chain;
	protected String name;
	protected String orderString;
	protected CombiningPolicy combiner;
	protected Properties properties = null;
	
	protected final List<ExternalSystemConnector>externalConnections = new ArrayList<>();

	protected void setup(Kernel kernel) {
		ContainerSecurityProperties csp = kernel.getContainerSecurityConfiguration();
		this.properties = csp.getRawProperties();
	}	
	/**
	 * will configure all the aips in the chain
	 */
	@Override
	public void configure(String name, Kernel kernel) throws ConfigurationException {
		this.name = name;
		chain = createChain(kernel);
	}
	
	@Override
	public String getStatusDescription() {
		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");
		if(chain.size()==0){
			sb.append("N/A");
		}
		else {
			for(T a: chain){
				sb.append(newline).append(" * ");
				if(a instanceof ISubSystem) {
					sb.append(((ISubSystem)a).getStatusDescription());
				}
				else{
					sb.append(a.toString());
				}
			}
			if(chain.size()>1){
				sb.append(newline);
				sb.append(" * combining policy: ").append(String.valueOf(combiner));
			}
		}
		return sb.toString();
	}
	
	@Override
	public Collection<ExternalSystemConnector>getExternalConnections(){
		return externalConnections;
	}
	
	public List<T> getChain(){
		return Collections.unmodifiableList(chain);
	}
	
	public CombiningPolicy getCombiningPolicy(){
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

	@SuppressWarnings("unchecked")
	protected Pair<List<T>, List<String>> createChain(String cspPrefix, Kernel kernel) throws ConfigurationException {
		List<T> newChain = new ArrayList<>();
		List<String> newNames = new ArrayList<>();
		if (orderString == null) {			
			String nn = name == null ? "" : "." + name;
			throw new ConfigurationException("Configuration inconsistent, " +
					"need to define <" + cspPrefix + nn + ".order>");
		}
		String[] authzNames=orderString.split(" +");
		for(String auth: authzNames){
			T aip = (T)AttributeSourceConfigurator.configureAS(auth, 
					cspPrefix, properties);
			aip.configure(auth, kernel);
			if(aip instanceof ExternalSystemConnector) {
				externalConnections.add((ExternalSystemConnector)aip);
			}
			newChain.add(aip);
			newNames.add(auth);
		}
		return new Pair<>(newChain, newNames);
	}

	protected abstract List<T> createChain(Kernel kernel);

	public void setCombiningPolicy(String combinerName) throws ConfigurationException {
		if("MERGE_LAST_OVERRIDES".equalsIgnoreCase(combinerName)){
			combiner = BaseAttributeSourcesChain.MERGE_LAST_OVERRIDES;
		}
		else if("MERGE".equalsIgnoreCase(combinerName)){
			combiner = BaseAttributeSourcesChain.MERGE;
		}
		else if("FIRST_APPLICABLE".equalsIgnoreCase(combinerName)){
			combiner = BaseAttributeSourcesChain.FIRST_APPLICABLE;
		}
		else if("FIRST_ACCESSIBLE".equalsIgnoreCase(combinerName)){
			combiner = BaseAttributeSourcesChain.FIRST_ACCESSIBLE;
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
	public static final CombiningPolicy FIRST_APPLICABLE = new CombiningPolicy() {

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
			return "FIRST_APPLICABLE";
		}
	};

	/**
	 * first accessible: the answer from the first accessible attribute source is used. It is
	 * assumed that AttributeSource throws an exception when there is communication error.
	 */
	public static final CombiningPolicy FIRST_ACCESSIBLE = new CombiningPolicy() {
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
			return "FIRST_ACCESSIBLE";
		}
	};

	
	/**
	 * merge_last_overrides:  new attributes overwrite existing ones
	 */
	public static final CombiningPolicy MERGE_LAST_OVERRIDES = new CombiningPolicy() {

		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			master.addAllOverwritting(newAttributes);
			return true;
		}

		public String toString(){
			return "MERGE_LAST_OVERRIDES";
		}
	};
	
	/**
	 * Merge:  attributes with the same key are combined (values are added).
	 * This is always OK for XACML attributes (only duplicates are not maintained) and
	 * for lists of valid values. However for regular incarnation attributes 
	 * (like xlogin, role, primary gid, queue) only one value make sense. So in case of 
	 * default values obtained from AS of incarnation attributes the same policy is 
	 * used as in MERGE_LAST_OVERRIDS. 
	 */
	public static final CombiningPolicy MERGE = new CombiningPolicy (){

		public boolean combineAttributes(SubjectAttributesHolder master, SubjectAttributesHolder newAttributes){
			master.addAllMerging(newAttributes);
			return true;
		}

		public String toString(){
			return "MERGE";
		}
	};

	@Override
	public String getName()
	{
		return "User mapping & user attributes";
	}
}
