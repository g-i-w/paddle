package paddle;

import java.net.Socket;
import java.util.Map;

public class InboundHTTP extends ConnectionTCP {

	private RequestHTTP request;
	private ResponseHTTP response;
	private Server server;
	
	public InboundHTTP (
		Server server,
		Socket socket,
		byte[] inboundMemory,
		int connectionId,
		int timeout
	) {
		super( server.state(), socket, server.name(), null, inboundMemory, connectionId, timeout, false );
		this.server = server;
		request = new RequestHTTP( inboundMemory );
	}
	
	public void received () {
		try {
			request.parse( inboundMemoryPlace() );
			super.received();
		} catch (Exception e) {
			e.printStackTrace();
			end();
		}
	}
	
	public void transmitted () {
		close();
		super.transmitted();
	}
	
	public RequestHTTP request () {
		return request;
	}
	
	public ResponseHTTP response () {
		return response;
	}
	
	public void response ( ResponseHTTP response ) {
		this.response = response;
		outboundMemory( response.document() );
		outboundMemoryValid();
	}
	
	public Server server () {
		return server;
	}
	
	public boolean inbound () {
		return true;
	}
	
	public String protocol () {
		return "HTTP";
	}
	
	public boolean complete () {
		return request.complete();
	}
	
	// remove unnecessary output:

	public void initSuccess () {}
		
}
