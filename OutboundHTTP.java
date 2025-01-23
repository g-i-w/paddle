package paddle;

import java.net.*;
import java.io.*;

public class OutboundHTTP extends ConnectionTCP {
	
	private RequestHTTP request;
	private ResponseHTTP response;
	private Server server;
	private int maxInboundMemory;
	
	// simple anonymous connection
	
	public OutboundHTTP ( String address, String path, int maxInboundMemory ) throws Exception {
		this( address, 80, "GET", path, new byte[4*1024*1024], maxInboundMemory ); // 4MiB
	}

	public OutboundHTTP ( String address, int port, String method, String path, int maxInboundMemory ) throws Exception {
		this( address, port, method, path, new byte[4*1024*1024], maxInboundMemory ); // 4MiB
	}

	public OutboundHTTP ( String address, int port, String method, String path, byte[] inboundMemory, int maxInboundMemory ) throws Exception {
		this (
			new ServerState(),
			new Socket( address, port ),
			method,
			path,
			null,
			null,
			inboundMemory,
			-1,
			10000, // 10 sec
			maxInboundMemory
		);
	}
	
	// connection from a Server
	
	public OutboundHTTP ( Server server, String address, int port, String method, String path, byte[] data, byte[] inboundMemory ) throws Exception {
		this( server.state(), new Socket( address, port ), method, path, null, data, inboundMemory, server.sequenceId(), 4000, -1 );
		this.server = server;
	}
	
	public OutboundHTTP ( Server server, InetAddress address, int port, InetAddress localAddress, int localPort, String method, String path, byte[] data, byte[] inboundMemory ) throws Exception {
		this( server.state(), new Socket ( address, port, localAddress, localPort ), method, path, null, data, inboundMemory, server.sequenceId(), 4000, -1 );
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
		int timeout,
		int maxInboundMemory
	) throws Exception {
		super( state, socket, method+" "+path, null, inboundMemory, connectionId, timeout, false );
		response = new ResponseHTTP( inboundMemory );
		request = new RequestHTTP( method, path, header, data );
		this.maxInboundMemory = maxInboundMemory;
		while( starting() ) Thread.sleep(1);
		request.header( "Host", remoteAddress()+":"+remotePort() );
		request.header( "Connection", "close" );
		outboundMemory( request.document() );
		outboundMemoryValid();
	}
	
	public void received () {
		try {
			response().parse( inboundMemoryPlace() );
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
	
	@Override
	protected void inboundMemoryFullEvent () {
		int currentLen = inboundMemory().length;
		if (maxInboundMemory > currentLen*2) {
			inboundMemory( currentLen*2 ); // if more than double, then just double it for now...
			//System.out.println( "*** doubling!" );
		} else if (maxInboundMemory > currentLen) {
			inboundMemory( maxInboundMemory ); // otherwise max it out
			//System.out.println( "*** maxing!" );
		}
		response.memory( inboundMemory() );
	}
	
	
	// testing
	public static void main ( String[] args ) throws Exception {
		OutboundHTTP http = new OutboundHTTP (
			args[0],
			( args.length > 1 ? args[1] : "/" ),
			100*1024*1024 // 100MiB max
		);
		while( !http.response().complete() ) Thread.sleep(100);
		//Thread.sleep(1000);
		System.out.println( new String( http.inboundMemory() ) );
	}
		
}
