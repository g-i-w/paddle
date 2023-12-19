package paddle;

import java.net.*;
import java.io.*;

public class InboundTCP extends ConnectionTCP {

	private Server server;
	private boolean verbose;


	public InboundTCP ( Server server, Socket socket, byte[] outboundMemory, byte[] inboundMemory, int connectionId, int timeout ) {
		this( server, socket, outboundMemory, inboundMemory, connectionId, timeout, true );
	}
	
	public InboundTCP ( Server server, Socket socket, byte[] outboundMemory, byte[] inboundMemory, int connectionId, int timeout, boolean verbose ) {
		super(
			server.state(),
			socket,
			server.getName()+" (client "+connectionId+") on port "+socket.getLocalPort(),
			outboundMemory,
			inboundMemory,
			connectionId,
			timeout,
			false
		);
		this.verbose = verbose;
		this.server = server;
	}
	

	// connection messages
	public void initSuccess () {
		if (verbose) System.out.println( this.getClass().getName()+" '"+getName()+"': connection from "+remoteAddress()+":"+remotePort()+"..." );
	}
	
	public void initException ( Exception e ) {
		if (verbose) System.out.println( this.getClass().getName()+" '"+getName()+"': Exception during connection from "+remoteAddress()+":"+remotePort()+":\n"+e );
		end();
	}

	
	// connection identity info
	public boolean inbound () {
		return true;
	}
	
	public Server server () {
		return server;
	}
	

	// test
	public static void main (String[] args) throws Exception {
		ServerState state = new ServerState();
		
		ServerTCP server0 = new ServerTCP( state, 9000, "server0", "Hi client!".getBytes(), 300 );
		
		OutboundTCP tcp0 = new OutboundTCP(
			state,
			"localhost",
			9000,
			"tcp0",
			"Hello server! how are you today?".getBytes(),
			new byte[300],
			100,
			4000,
			true,
			true
		);
		
		Thread.sleep(1000);
		
		System.out.println(
			"Text received: "+
			(new OutboundTCP( "localhost", 9000, "test data" ))
			.receive()
			.text()
		);
		
	}
	
}
