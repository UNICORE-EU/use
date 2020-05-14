/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package eu.unicore.uas.security.ldap;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class holding usage statistics, enabled by JMX interface.
 * @author K. Benedyczak
 */
public class JMXStats
{
	private int totalAuth=0;
	private int cacheHits=0;
	private float meanAccessTime=0;
	private Set<String> accessorNames=new HashSet<String>();

	public synchronized void incTotalAuth()
	{
		totalAuth++;
	}
	
	public synchronized void incCacheHits()
	{
		cacheHits++;
	}
	
	public synchronized void addAccessor(String name)
	{
		accessorNames.add(name);
	}
	
	public synchronized void publishTime(long time){
		meanAccessTime=(meanAccessTime*(totalAuth-1)+ time)/(totalAuth);
	}
	
	public synchronized void clearStatistics(){
		totalAuth=0;
		meanAccessTime=0f;
		cacheHits=0;
		accessorNames.clear();
	}

	public synchronized void clearRequestorNames(){
		accessorNames.clear();
	}

	
	public synchronized int getTotalRequests()
	{
		return totalAuth;
	}

	public synchronized int getCacheHits()
	{
		return cacheHits;
	}

	public synchronized float getMeanProcessingTime()
	{
		return meanAccessTime;
	}
	
	public synchronized String[] getRequestorNames()
	{
		return accessorNames.toArray(new String[accessorNames.size()]);
	}
}
