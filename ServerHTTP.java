package paddle;

import java.util.*;
import java.io.*;
import java.net.*;


public class ServerHTTP extends Server implements Node {

	private ServerSocket serverSocket;
	private int memorySize;
	private int timeout;
	
	public ServerHTTP ( ServerState state, int port, String name ) {
		this(state, port, name, 1024, 4000 );
	}
	
	public ServerHTTP ( ServerState state, int port, String name, int memorySize, int timeout ) {
		super(state, port, name);
		this.memorySize = memorySize;
		this.timeout = timeout;
	}
	
	// Exception is caught by abstract class Server
	public void init () throws Exception {
		serverSocket = new ServerSocket( this.port() );
	}
	
	// Exception is caught by abstract class Server
	public void loop () throws Exception {
		Socket socket = serverSocket.accept();  // Wait for a client to connect
		//System.out.println( "here: "+socket );
		new InboundHTTP(
			this,
			socket,
			new byte[memorySize],
			sequenceId(),
			timeout
		);  // Handle the client in a separate thread
	}
	
	public Connection send ( String remoteAddress, int remotePort, byte[] outboundData ) throws Exception {
		return new OutboundHTTP( this, remoteAddress, remotePort, "POST", "/", outboundData, new byte[memorySize] );
	}
	
	public Connection reply ( Connection c, byte[] outboundData ) throws Exception {
		InboundHTTP inbound = (InboundHTTP)c;
		inbound.response( new ResponseHTTP( null, outboundData ) );
		return c;
	}
	
	public Connection forward ( Connection c, String address, int port ) throws Exception {
		ConnectionTCP tcp = (ConnectionTCP)c;
		return new OutboundTCP ( address, port, tcp );
	}
	
	// Testing
	public static void main ( String[] args ) throws Exception {
		
		ServerHTTP http0 = new ServerHTTP (
			new TestServerHTTP(),
			11111,
			"test HTTP server",
			1024,
			4000
		);
		
		while( http0.starting() ) Thread.sleep(1);
		Thread.sleep( 1000 );
		
		http0.send( "localhost", 11111, "hello HTTP server!".getBytes() );
		
		System.out.println( http0 );
	}

}


class TestServerHTTP extends ServerState {

	public void received ( Connection c ) {
		print( c );
		if (c instanceof InboundHTTP) {
			InboundHTTP session = (InboundHTTP)c;
			session.response(
				new ResponseHTTP( "It works!\npath: '"+session.request().path()+"'" )
			);
		} else if (c instanceof OutboundHTTP) {
			OutboundHTTP session = (OutboundHTTP)c;
			System.out.println(
				"----\nResponse:\n\n"+
				(new String(session.response().data()))+
				"\n----"
			);
		}
	}
	
}
