package paddle;

import java.net.*;
import java.io.*;

public class OutboundHTTP extends ConnectionTCP {
	
	private RequestHTTP request;
	private ResponseHTTP response;
	private Server server;
	
	// simple anonymous connection
	
	public OutboundHTTP ( String address, int port, String method, String path ) throws Exception {
		this (
			new ServerState(),
			new Socket( address, port ),
			method,
			path,
			null,
			null,
			new byte[65536],
			-1,
			4000
		);
	}
	
	// connection from a Server
	
	public OutboundHTTP ( Server server, String address, int port, String method, String path, byte[] data, byte[] inboundMemory ) throws Exception {
		this( server.state(), new Socket( address, port ), method, path, null, data, inboundMemory, server.sequenceId(), 4000 );
		this.server = server;
	}
	
	public OutboundHTTP ( Server server, InetAddress address, int port, InetAddress localAddress, int localPort, String method, String path, byte[] data, byte[] inboundMemory ) throws Exception {
		this( server.state(), new Socket ( address, port, localAddress, localPort ), method, path, null, data, inboundMemory, server.sequenceId(), 4000 );
		this.server = server;
	}
	
	// full featured connection
	
	public OutboundHTTP (
		ServerState state,
		Socket socket,
		String method,
		String path,
		String[] header,
		byte[] data,
		byte[] inboundMemory,
		int connectionId,
		int timeout
	) throws Exception {
		super( state, socket, method+" "+path, null, inboundMemory, connectionId, timeout, false );
		response = new ResponseHTTP( inboundMemory );
		request = new RequestHTTP( method, path, header, data );
		while( starting() ) Thread.sleep(1);
		request.header( "Host", remoteAddress()+":"+remotePort() );
		request.header( "Connection", "close" );
		outboundMemory( request.document() );
		outboundMemoryValid();
	}
	
	public void received () {
		try {
			response.parse( inboundMemoryPlace() );
			if (response.complete()) close();
			super.received();
		} catch (Exception e) {
			e.printStackTrace();
			end();
		}
	}
	
	public RequestHTTP request () {
		return request;
	}
	
	public ResponseHTTP response () {
		return response;
	}
	
	public Server server () {
		return server;
	}
	
	public boolean inbound () {
		return false;
	}
	
	public String protocol () {
		return "HTTP";
	}
	
	
	// testing
	public static void main ( String[] args ) throws Exception {
		/*new OutboundHTTP (
			new ServerState(),
			new Socket( args[0], ( args.length > 1 ? Integer.parseInt(args[1]) : 80 ) ),
			"GET",
			"/",
			null,
			null,
			new byte[65536],
			-1,
			4000
		);*/
		new OutboundHTTP (
			args[0],
			( args.length > 1 ? Integer.parseInt(args[1]) : 80 ),
			"GET",
			"/"
		);
	}
		
}
