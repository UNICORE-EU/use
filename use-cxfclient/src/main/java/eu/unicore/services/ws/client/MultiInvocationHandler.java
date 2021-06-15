package eu.unicore.services.ws.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/**
 * Proxy invocation handler calling underlying proxies in a multicast
 * or round-robin fashion<br>
 * 
 * @see MultiWSRFClient
 * @author schuller
 */
public class MultiInvocationHandler<Target> implements InvocationHandler {

	private int mode=MultiWSRFClient.MULTICAST;
	
	private List<Target>targets;
	
	private Random rand=new Random();
	
	private MultiWSRFClient<?>client;
	
	public MultiInvocationHandler(MultiWSRFClient<?> client){
		this.client=client;
	}
	
	public void setMode(int mode){
		this.mode=mode;
	}
	
	public void setTargets(List<Target> clients){
		this.targets=clients;
	}
	
	/**
	 * invokes the given method on all or a subset of the 
	 * clients
	 */
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		boolean errors=false;
		if(mode==MultiWSRFClient.MULTICAST){
			Object last=null;
			for(Target t: targets){
				try{
					last=method.invoke(t, args);
				}catch(Exception e){
					//TODO
					errors=true;
				}
			}
			client.setErrorsOccurred(errors);
			return last;
		}
		else if (mode==MultiWSRFClient.ROUNDROBIN){
			int next=rand.nextInt(targets.size());
			return method.invoke(targets.get(next), args);
		}
		
		else if (mode==MultiWSRFClient.ROUNDROBIN_RETRY_ON_FAILURE){
			int c=0;
			while(true && c<client.getMaxRetries()){
				int next=rand.nextInt(targets.size());
				try{
					c++;
					return method.invoke(targets.get(next), args);
				}catch(Exception e){}
			}
		}
		return null;
	}

}
