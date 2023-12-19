package paddle;

import java.net.*;

public class InboundUDP extends Thread implements Connection {

	private ServerUDP server;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private int packetId;
	private byte[] data;
	private String remoteAddress;
	private int remotePort;
	private boolean complete = false;
	

	public InboundUDP ( ServerUDP server, DatagramSocket socket, DatagramPacket packet, int packetId ) {
		this.server = server;
		this.socket = socket;
		this.packet = packet;
		this.packetId = packetId;
		
		start();
	}
	
	public void run () {
		data = packet.getData();
		complete = true;
		server.state().respond( this );
	}
	
	// specific to InboundUDP (returns an OutboundUDP object)
	public OutboundUDP reply ( String replyText ) throws Exception {
		return reply ( replyText.getBytes() );
	}

	public OutboundUDP reply ( byte[] replyData ) throws Exception {
		return new OutboundUDP (
			remoteAddress(),
			remotePort(),
			replyData,
			server,
			server.sequenceId()
		);
	}
	
	// network info
	public String remoteAddress () {
		return packet.getAddress().toString().substring(1);
	}
	public int remotePort () {
		return packet.getPort();
	}
	public String localAddress () {
		return socket.getLocalAddress().toString();
	}
	public int localPort () {
		return socket.getPort();
	}
	
	// connection identity info
	public String protocol () {
		return "UDP";
	}
	
	public boolean inbound () {
		return true;
	}
	
	public boolean complete () {
		return complete;
	}
	
	public Server server () {
		return server;
	}
	public int connectionId () {
		return packetId;
	}
	
	public byte[] inboundData () {
		return data;
	}
	
	public byte[] outboundData () {
		return null;
	}
		
}
