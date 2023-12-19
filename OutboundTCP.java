package paddle;

import java.net.*;
import java.io.*;

public class OutboundTCP extends ConnectionTCP {

	private boolean verbose;

	// Annonymously send String immediately (simple)
	public OutboundTCP ( String address, int port, String outboundText ) throws Exception {
		this( new ServerState(), address, port, "OutboundTCP", outboundText.getBytes(), new byte[1024], -1, 4000, true, true );
	}

	// Send String immediately (simple)
	public OutboundTCP ( ServerState state, String address, int port, String outboundText ) throws Exception {
		this( state, address, port, "OutboundTCP", outboundText.getBytes(), new byte[1024], -1, 4000, true, true );
	}

	// Send String immediately
	public OutboundTCP ( ServerState state, String address, int port, String name, String outboundText, int inboundLength, int connectionId ) throws Exception {
		this( state, address, port, name, outboundText.getBytes(), new byte[inboundLength], connectionId, 4000, true, true );
	}

	// Send bytes incrementally
	public OutboundTCP ( ServerState state, String address, int port, String name, byte[] outboundMemory, byte[] inboundMemory, int connectionId ) throws Exception {
		this( state, address, port, name, outboundMemory, inboundMemory, connectionId, 10000, false, true );
	}

	// Send bytes immediately (simple)
	public OutboundTCP ( ServerState state, String address, int port, byte[] outboundBytes ) throws Exception {
		this( state, address, port, "OutboundTCP", outboundBytes, new byte[outboundBytes.length], -1, 4000, true, true );
	}
	
	// All-argument constructor (random local port, default address)
	public OutboundTCP ( ServerState state, String address, int port, String name, byte[] outboundMemory, byte[] inboundMemory, int connectionId, int timeout, boolean allValid, boolean verbose ) throws Exception {
		super( state, new Socket ( address, port ), name, outboundMemory, inboundMemory, connectionId, timeout, allValid );
		this.verbose = verbose;
	}
	
	// All-argument constructor
	public OutboundTCP ( ServerState state, InetAddress address, int port, InetAddress localAddress, int localPort, String name, byte[] outboundMemory, byte[] inboundMemory, int connectionId, int timeout, boolean allValid, boolean verbose ) throws Exception {
		super( state, new Socket ( address, port, localAddress, localPort ), name, outboundMemory, inboundMemory, connectionId, timeout, allValid );
		this.verbose = verbose;
	}
	
	// Relay constructor (general purpose, may require calling update( ConnectionTCP ) on first connection)
	public OutboundTCP ( ServerState state, String address, int port, String name, byte[] outboundMemory, byte[] inboundMemory, int connectionId, int timeout, ConnectionTCP c ) throws Exception {
		this( state, address, port, name, outboundMemory, inboundMemory, connectionId, timeout, false, true );
		this.synchronize( c ); // this --> other
	}

	// Relay constructor (2-way simple)
	public OutboundTCP ( String address, int port, ConnectionTCP c ) throws Exception {
		this(
			c.server().state(),
			address,
			port,
			c.remoteAddress()+":"+c.remotePort()+"<--["+c.connectionId()+"]-->"+address+":"+port,
			c.inboundMemory(),  // in --> out
			c.outboundMemory(), // out --> in
			c.connectionId(),
			c.timeout(),
			c
		);
		c.synchronize( this ); // other --> this
	}

	
	// connection messages
	public void initSuccess () {
		if (verbose) System.out.println( this.getClass().getName()+" '"+getName()+"': connected to "+remoteAddress()+":"+remotePort()+"..." );
	}
	
	public void initException ( Exception e ) {
		System.out.println( this.getClass().getName()+" '"+getName()+"': Exception while connecting to "+remoteAddress()+":"+remotePort()+"\n"+e );
		end();
	}
	
	// connection identity info
	public boolean inbound () {
		return false;
	}
	
	public Server server () {
		return this;
	}
	
	// test
	public static void main (String[] args) throws Exception {
		ServerState state = new ServerState();
		
		ServerHTTP http0 = new ServerHTTP( state, 9000, "http0" );
		
		OutboundTCP tcp0 = new OutboundTCP(
			state,
			"localhost",
			9000,
			"tcp0",
			"GET /?some_data HTTP/1.1\r\n\r\n".getBytes(),
			new byte[300],
			100
		);

		while(http0.starting()) Thread.sleep(1);
		
		tcp0.outboundMemoryValid();
		
		Thread.sleep(1000);
		
		System.out.println(
			"Text received: "+
			(new OutboundTCP( "localhost", 9000, "GET /easy HTTP/1.1\r\n\r\n" ))
			.receive()
			.text()
		);
		
		Thread.sleep(1000);
		
		tcp0.end();
		
	}
	
}
