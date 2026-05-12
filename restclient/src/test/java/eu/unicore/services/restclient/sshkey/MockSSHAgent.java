package eu.unicore.services.restclient.sshkey;

import static java.net.StandardProtocolFamily.UNIX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.services.restclient.sshkey.SSHAgentProxy.Identity;

public class MockSSHAgent {

	private final String path;
	private volatile boolean stopped = false;
	List<Identity> identities = new ArrayList<>();
	String keyFile = null;

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
	
	private void handle(SocketChannel channel) throws Exception {
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
				len = payload.readInt();
				payload.readNBytes(len);
				len = payload.readInt();
				byte[] toSign = payload.readNBytes(len);
				// sig type
				int flags = payload.readInt();
				assert flags == SSH_AGENT_RSA_SHA2_256;
				byte[] sig = doSign(toSign);
				response.writeByte(SSH2_AGENT_SIGN_RESPONSE);
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
	
	private byte[] doSign(byte[] data) throws Exception {
		if(keyFile==null) {
			return "mock signature".getBytes();
		}
		PrivateKey pk = SSHUtils.readPrivateKey(new File(keyFile), ()-> "test123".toCharArray());
		Signature sig = Signature.getInstance(pk.getAlgorithm());
		sig.initSign(pk);
		sig.update(data);
		System.err.println("Signing with <"+keyFile+">");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		byte[] algo = "ssh-ed25519".getBytes();
		dos.writeInt(algo.length);
		dos.write(algo);
		byte[] _sig = sig.sign();
		dos.writeInt(_sig.length);
		dos.write(_sig);
		dos.flush();
		return bos.toByteArray();
	}

	// constants used in the agent protocol
	private static final byte SSH_AGENT_FAILURE = 5;
	private static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
	private static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
	private static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
	private static final byte SSH2_AGENT_SIGN_RESPONSE = 14;
	private static final byte SSH_AGENT_RSA_SHA2_256 = 0x02;
}
