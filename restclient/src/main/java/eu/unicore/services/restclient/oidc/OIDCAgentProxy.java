package eu.unicore.services.restclient.oidc;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnixDomainSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/**
 * Connector to the 'oidc-agent' via UNIX domain socket.
 *
 * @author schuller
 */
public class OIDCAgentProxy {

	private static final String OIDC_SOCK = "OIDC_SOCK";

	public static boolean isConnectorAvailable(){
		return System.getenv(OIDC_SOCK)!=null;
	}
	private final String path;
	
	OIDCAgentProxy(String path){
		this.path = path;
	}
	
	public OIDCAgentProxy(){
		this(System.getenv(OIDC_SOCK));
	}

	public String send(String data) throws Exception {
		UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(path);
		try(SocketChannel channel =  SocketChannel.open(unixSocketAddress);
		    PrintWriter w = new PrintWriter(Channels.newOutputStream(channel));
        	InputStreamReader r = new InputStreamReader(Channels.newInputStream(channel)))
        {
        	w.print(data);
        	w.flush();
        	CharBuffer result = CharBuffer.allocate(4096);
        	r.read(result);
        	return result.flip().toString();
        }
		catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
