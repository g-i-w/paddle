package paddle;

public class ExampleServerState extends ServerState {
	
	public void received ( Connection c ) {
		if (c instanceof InboundTCP) {
			System.out.println(
				"Received TCP data..."
			);
		} else if (c instanceof InboundHTTP) {
			InboundHTTP session = (InboundHTTP)c;
			if (session.request().path().indexOf("/favicon")==0) return;
			if (session.request().complete()) {
				System.out.println( session.request() );
				session.response(
					new ResponseHTTP(
						"200",
						"OK",
						new String[]{ "Content-Type", "text/html" },
						("<h1>HTTP works!</h1>"+
						"<br><table>"+
						"<tr><td>path</td><td>"+session.request().path()+"</td></tr>"+
						"<tr><td>data</td><td>"+(new String(session.request().data()))+"</td></tr>"+
						"</table>").getBytes()
					)
				);
			}
		} else if (c instanceof InboundUDP) {
			InboundUDP rxPacket = (InboundUDP)c;
			try {
				Thread.sleep(100);
				OutboundUDP txPacket = rxPacket.reply(
					"UDP works! Received data ["+rxPacket.connectionId()+"]: '"+(new String(rxPacket.inboundData())+"'")
				);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println( "["+c.connectionId()+"] OTHER CONNECTION TYPE" );
			print( c );
		}
	}
	
	// Example servers
	public static void main ( String[] args ) {
	
		// HTTP
		ServerHTTP http0 = new ServerHTTP (
			new ServerState(),
			11111,
			"test HTTP server",
			1024,
			4000
		);
		
		
	}
	
	
}
