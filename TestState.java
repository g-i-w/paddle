package paddle;

/*
- A ServerState instance is a vehicle for maintaining the state of the server in
  one object.
- This one object is passed to each Session of each Server.
*/

public class TestState extends ServerState {

	private ServerUDP udp;
	private ServerHTTP http;

	public TestState ( int udpPort, int httpPort ) {
		this.udp = new ServerUDP( this, udpPort, "udp" );
		this.http = new ServerHTTP( this, httpPort, "http" );
		while(http.starting() || udp.starting()) {
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void respond ( InboundHTTP session ) {
		handle( session );
		session.response().setBody(
			"<h1>HTTP works!</h1>\n<br>\n"+
			"path: "+session.request().path()+"\n<br>\n"+
			"body: "+session.request().body()+"\n<br>"
		);
		try {
			udp.forward( session, "localhost", udp.port() );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void respond ( InboundUDP rxPacket ) {
		handle( rxPacket );
		try {
			Thread.sleep(500);
			OutboundUDP txPacket = rxPacket.reply(
				"UDP works! Received data ["+rxPacket.connectionId()+"]: '"+(new String(rxPacket.inboundData())+"'")
			);
			handle( txPacket );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main (String[] args) throws Exception {
		ServerState state = new TestState( 9000, 8000 );
		
		OutboundTCP tcp0 = new OutboundTCP(
			state,
			"localhost",
			8000,
			"tcp0",
			"GET /?some_data HTTP/1.1\r\n\r\n".getBytes(),
			new byte[300],
			100,
			4000,
			true,
			true
		);
	}

}
