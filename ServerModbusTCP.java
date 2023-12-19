package paddle;

public class ServerModbusTCP extends ServerTCP {


	public ServerModbusTCP ( ServerState state, int port, String name, byte[] outbound, int inboundSize ) {
		super( state, port, name, outbound, inboundSize );
	}

	// test
	public static void main (String[] args) throws Exception {
	
		ServerState state = new ServerState();

		ServerModbusTCP m0 = new ServerModbusTCP( state, 502, "m0", new byte[]{0x01,0x02,0x03,0x04}, 16 );
		
		while(m0.starting()) Thread.sleep(1);
		
		System.out.println(
			(new OutboundTCP( "localhost", 502, "Hi server0!" ))
			.receive()
			.text()
		);
		
	}

}
