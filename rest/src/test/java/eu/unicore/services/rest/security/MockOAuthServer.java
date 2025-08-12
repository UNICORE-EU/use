package eu.unicore.services.rest.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;

public class MockOAuthServer implements Runnable {

	private static final Logger log = Log.getLogger(Log.SECURITY, MockOAuthServer.class);

	private final int port;

	private ServerSocket serverSocket;

	private volatile boolean stopping=false;

	private volatile boolean stopped=false;

	private String answer = "";
	private String contenttype = "application/json";

	/**
	 * creates a mock IDP listening on a free port
	 */
	public MockOAuthServer()throws IOException{
		serverSocket=new ServerSocket(0);
		serverSocket.setSoTimeout(5000);
		this.port=serverSocket.getLocalPort();
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
				readLines(socket.getInputStream());
				writeReply(socket);
			}catch(SocketTimeoutException te) {}
			catch(Exception ex){
				System.out.println("EX: "+ex.getClass().getName());
			}
		}
		log.info("Stopped.");
		stopped=true;
		IOUtils.closeQuietly(serverSocket);
		serverSocket=null;
	}

	private void writeReply(Socket socket)throws Exception{
		String status="HTTP/1.1 200 OK\n";
		StringBuilder reply = new StringBuilder();
		reply.append("Content-Type: ").append(contenttype).append("\n");
		reply.append("Content-Length: "+answer.length()+"\n");
		reply.append("\n"+answer);
		socket.getOutputStream().write(status.getBytes());
		socket.getOutputStream().write(reply.toString().getBytes());
	}

	public void setAnswer(String answer){
		this.answer = answer;
	}

	public void setContentType(String ct){
		this.contenttype = ct;
	}

	private List<String> readLines(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		List<String> list = new ArrayList<>();
		String line;
		while ( (line = reader.readLine()) != null) {
			if(line.isEmpty()) {
				break;
			}
			list.add(line);
		}
		return list;
	}
}
