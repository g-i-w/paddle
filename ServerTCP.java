package paddle;

import java.util.*;
import java.io.*;
import java.net.*;


public class ServerTCP extends Server implements Node {

	private ServerSocket serverSocket;
	private byte[] outboundMemory;
	private int outboundMemorySize;
	private int inboundMemorySize;
	
	public ServerTCP ( ServerState state, int port, String name, byte[] outboundMemory, int inboundMemorySize ) {
		super(state, port, name);
		this.outboundMemory = outboundMemory;
		this.inboundMemorySize = inboundMemorySize;
		
	}
	
	public ServerTCP ( ServerState state, int port, String name, int outboundMemorySize, int inboundMemorySize ) {
		super(state, port, name);
		this.outboundMemorySize = outboundMemorySize;
		this.inboundMemorySize = inboundMemorySize;
	}

	// Exception is caught by abstract class Server
	public void init () throws Exception {
		serverSocket = new ServerSocket( port() );
	}
	
	// Exception is caught by abstract class Server
	public void loop () throws Exception {
		Socket socket = serverSocket.accept();  // Wait for a client to connect
		InboundTCP client = new InboundTCP(
			this,
			socket,
			(outboundMemory != null ? outboundMemory : new byte[outboundMemorySize]),
			new byte[inboundMemorySize],
			sequenceId(), // atomic int from Server.java
			10000 // 10 seconds
		);  // Handle the client in a separate thread
		if (outboundMemory != null) client.outboundMemoryValid();
	}
	
	
	public Connection send ( String remoteAddress, int remotePort, byte[] outboundData ) throws Exception {
		return new OutboundTCP( state(), remoteAddress, remotePort, outboundData );
	}
	
	public Connection reply ( Connection c, byte[] outboundData ) throws Exception {
		ConnectionTCP tcp = (ConnectionTCP)c;
		tcp.outboundMemoryWrite( outboundData );
		return c;
	}
	
	public Connection forward ( Connection c, String remoteAddress, int remotePort ) throws Exception {
		if (!c.complete() && c instanceof ConnectionTCP) {
			ConnectionTCP tcp = (ConnectionTCP)c;
			return new OutboundTCP ( remoteAddress, remotePort, tcp );
		} else {
			return new OutboundTCP( state(), remoteAddress, remotePort, c.outboundData() );
		}
	}
	
	// test
	public static void main (String[] args) throws Exception {
	
		ServerState state = new ServerState();

		ServerTCP server0 = new ServerTCP( state, 9000, "server0", "Hi, this is server0!".getBytes(), 16 );
		ServerTCP server1 = new ServerTCP( state, 9001, "server1", "Hi client, this is server1!".getBytes(), 16 );
		
		while(server0.starting()) Thread.sleep(1);
		
		System.out.println(
			(new OutboundTCP( "localhost", 9000, "Hi server0!" ))
			.sendAndClose()
			.text()
		);
		
		Thread.sleep(2000);
		
		System.out.println(
			(new OutboundTCP( "localhost", 9001, "Hi server1!" ))
			.capture(10)
			.text()
		);
		
				
	}
	
}
