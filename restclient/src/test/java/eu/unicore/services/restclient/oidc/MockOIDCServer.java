package eu.unicore.services.restclient.oidc;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

/**
 * a fake server replying to HTTP requests
 */
public class MockOIDCServer implements Runnable {

	private final int port;

	private ServerSocket serverSocket;

	private volatile boolean stopping=false;

	private volatile boolean stopped=false;

	private volatile int statusCode=200;

	private String answer = "";

	private List<String> lastRequest = null;
	
	private Map<String,String> params;
	
	public boolean readContent = true;
			
	public String content = "";

	/**
	 * creates a FakeServer listening on the given port
	 * @param port
	 * @throws IOException
	 */
	public MockOIDCServer(int port)throws IOException{
		serverSocket=new ServerSocket(port);
		serverSocket.setSoTimeout(5000);
		this.port=serverSocket.getLocalPort();
	}

	/**
	 * creates a FakeServer listening on a free port
	 * @see getPort()
	 */
	public MockOIDCServer()throws IOException{
		this(0);
	}

	public String getURI(){
		return "http://localhost:"+port;
	}

	private static int n=0;
	public synchronized void start(){
		Thread t=new Thread(this);
		t.setName("FakeServer"+(n++));
		t.start();
	}

	public synchronized void restart()throws Exception{
		if(serverSocket!=null)throw new IllegalStateException();

		serverSocket=new ServerSocket(port);
		stopping=false;
		stopped=false;
		start();
	}

	public void stop(){
		stopping=true;
	}

	public boolean isStopped(){
		return stopped;
	}

	public void run(){
		while(!stopping){
			try(Socket socket = serverSocket.accept()){
				lastRequest = readLines(socket.getInputStream());
				params = decode( lastRequest.get(lastRequest.size()-1));
				writeReply(socket);
			}catch(SocketTimeoutException te) {}
			catch(Exception ex){
				System.out.println("EX: "+ex.getClass().getName());
			}
		}
		stopped=true;
		IOUtils.closeQuietly(serverSocket);
		serverSocket=null;
	}

	private void writeReply(Socket socket)throws Exception{
		String status="HTTP/1.1 "+statusCode+" some reason";
		JSONObject r = new JSONObject();
		if("password".equals(params.get("grant_type"))){
			r.put("access_token", "the_access_token");
			r.put("refresh_token", "the_refresh_token");
		}
		else if("refresh_token".equals(params.get("grant_type"))){
			r.put("access_token", "the_new_access_token");
			r.put("refresh_token", "the_refresh_token");
		}
		answer = r.toString();
		String reply="\nContent-Length: "+answer.length()+"\n\n"+answer;
		socket.getOutputStream().write(status.getBytes());
		socket.getOutputStream().write(reply.getBytes());
	}

	private List<String> readLines(InputStream in) throws IOException {
		byte[] buf = new byte[1024];
		int r = in.read(buf);
		String msg = new String(buf, 0, r, "UTF-8");
		List<String> list = Arrays.asList(msg.split("\\r\\n"));
		return list;
	}
	
	private Map<String,String>decode(String line){
		Map<String,String> res = new HashMap<>();
		for(String kv: Arrays.asList(line.split("&"))) {
			String[] t = kv.split("=");
			res.put(t[0],t[1]);
		}
		return res;
	}
	
}
