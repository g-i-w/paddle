package paddle;

import java.net.*;

public class OutboundUDP implements Connection {

	private ServerUDP server;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private String address;
	private int port;
	private byte[] data;
	private int packetId;
	private boolean complete = false;

	// independent of a Server (send text)
	public OutboundUDP ( String address, int port, String text ) throws Exception {
		this( address, port, text.getBytes(), new DatagramSocket(), -1 );
	}

	// independent of a Server (send byte array)
	public OutboundUDP ( String address, int port, byte[] data ) throws Exception {
		this( address, port, data, new DatagramSocket(), -1 );
	}

	// independent of a Server; with packetId (send byte array)
	public OutboundUDP ( String address, int port, byte[] data, int packetId ) throws Exception {
		this( address, port, data, new DatagramSocket(), packetId );
	}

	// via a Server (send text)
	public OutboundUDP ( String address, int port, String text, ServerUDP server ) throws Exception {
		this( address, port, text.getBytes(), server, -1 );
	}

	// via a Server; with packetId (send byte array)
	public OutboundUDP ( String address, int port, byte[] data, ServerUDP server, int packetId ) throws Exception {
		while (server.starting()) { // wait if the server is still starting up...
			Thread.sleep(1);
		}

		this.server = server;
		this.socket = server.socket();
		this.address = address;
		this.port = port;
		this.data = data;
		this.packetId = packetId;
		send();
	}

	// via a pre-constructed DatagramSocket (send byte array)
	public OutboundUDP ( String address, int port, byte[] data, DatagramSocket socket, int packetId ) throws Exception {
		this.socket = socket;
		this.address = address;
		this.port = port;
		this.data = data;
		this.packetId = packetId;
		send();
	}

	public OutboundUDP send ( byte[] newData ) throws Exception {
		data = newData;
		return send();
	}

	public OutboundUDP send () throws Exception {
		packet = new DatagramPacket(
			data,
			data.length,
			InetAddress.getByName( address ),
			port
		);
		socket.send( packet );
		complete = true;
		return this;
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
		return false;
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
		return null;
	}
	
	public byte[] outboundData () {
		return data;
	}
		
}
