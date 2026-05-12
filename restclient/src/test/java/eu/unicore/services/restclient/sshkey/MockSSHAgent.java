package eu.unicore.services.restclient.sshkey;

import static java.net.StandardProtocolFamily.UNIX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.services.restclient.sshkey.SSHAgentProxy.Identity;

public class MockSSHAgent {

	private final String path;
	private volatile boolean stopped = false;
	List<Identity> identities = new ArrayList<>();

	public MockSSHAgent(String path) {
		this.path = path;
	}

	public void start() {
		try {
			UnixDomainSocketAddress add = UnixDomainSocketAddress.of(path);
			System.out.println("Starting MockSSHAgent listening on <"+path+">");
			ServerSocketChannel srv = ServerSocketChannel.open(UNIX);
			srv.bind(add);
			while(!stopped) {
				SocketChannel ch = srv.accept();
				if(ch!=null) {
					handle(ch);
				}
				Thread.sleep(100);
			}
			System.out.println("MockSSHAgent exiting.");
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void stop() throws Exception {
		this.stopped = true;
		Thread.sleep(100);
	}
	
	private void handle(SocketChannel channel) throws IOException {
		try(DataOutputStream os = new DataOutputStream(Channels.newOutputStream(channel));
				DataInputStream is = new DataInputStream(Channels.newInputStream(channel)))
		{
			int l = is.readInt();
			byte[]data = new byte[l];
			int len = is.read(data, 0, l);
			assert len == l;
			DataInputStream payload = new DataInputStream(new ByteArrayInputStream(data));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream response = new DataOutputStream(bos);
			byte opcode = payload.readByte();
			if(SSH2_AGENTC_REQUEST_IDENTITIES==opcode) {
				response.writeByte(SSH2_AGENT_IDENTITIES_ANSWER);
				response.writeInt(identities.size());
				for(Identity i: identities) {
					response.writeInt(i.getBlob().length);
					response.write(i.getBlob());
					response.writeInt(i.getComment().length);
					response.write(i.getComment());
				}
			}
			else if(SSH2_AGENTC_SIGN_REQUEST==opcode) {
				response.writeByte(SSH2_AGENT_SIGN_RESPONSE);
				byte[] sig = "mock signature".getBytes();
				response.writeInt(sig.length);
				response.write(sig);
			}
			else {
				response.writeByte(SSH_AGENT_FAILURE);
			}
			response.flush();
			os.writeInt(bos.size());
			os.write(bos.toByteArray());
			os.flush();
		}
	}

	// constants used in the agent protocol
	private static final byte SSH_AGENT_FAILURE = 5;
	private static final byte SSH_AGENT_SUCCESS = 6;
	private static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
	private static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
	private static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
	private static final byte SSH2_AGENT_SIGN_RESPONSE = 14;
	private static final byte SSH_AGENT_RSA_SHA2_256 = 0x02;
}
