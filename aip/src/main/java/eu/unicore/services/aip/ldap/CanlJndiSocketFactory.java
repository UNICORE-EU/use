package eu.unicore.services.aip.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;


/**
 * Factory class for SocketFactories configured with canl. 
 * Unfortunately must be static due to JNDI API, however we use
 * a thread local storage to mostly eliminate problems related to classic static
 * singletons.
 * @author K. Benedyczak
 */
public class CanlJndiSocketFactory extends SocketFactory
{
	private static final ThreadLocal<SocketFactory> localFactory = new ThreadLocal<> ();
	
	//factory of factories, ughhh
	public static SocketFactory getDefault() 
	{
		return new CanlJndiSocketFactory();
	}

	private static SocketFactory getSPI() 
	{
		return localFactory.get();
	}
	
	public static void setImplementation(SocketFactory impl)
	{
		localFactory.set(impl);
	}
	
	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException
	{
		return getSPI().createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
			throws IOException, UnknownHostException
	{
		return getSPI().createSocket(host, port, localHost, localPort);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException
	{
		return getSPI().createSocket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
			int localPort) throws IOException
	{
		return getSPI().createSocket(address, port, localAddress, localPort);
	}
}
