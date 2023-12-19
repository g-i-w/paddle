package paddle;

public class OutboundModbusTCP extends OutboundTCP {
	private Bytes outbound;
	private Bytes inbound;
	
	// length in bytes including MBAP header fields ("words" are 2 bytes)
	public static int inboundTotalLength ( int f, int length ) {
		if ( f == 0 || f == 1 || f == 5 || f == 15 ) {
			return 12 + length/8 + 1; // discrete coils
		} else {
			return 12 + length*2; // registers
		}
	}

	private void initBytes ( int transactionId, int unitId, int function, int start ) {
		outbound = new Bytes( outboundMemory() );
		outbound
			.writeShortBE(	(short)transactionId,	0, 2 )
			.writeShortBE(	(short)0,		2, 2 )
			.write(		(byte)unitId,		6 )
			.write(		(byte)function,		7 )
			.writeShortBE(	(short)start,		8, 2 )
		;
		byte[] im = inboundMemory();
		for (int i=0; i<im.length; i++) {
			im[i] = 0;
		}
		inbound = new Bytes( im );
	}

	// read request
	public OutboundModbusTCP ( ServerState state, String address, int port, int transactionId, int unitId, int function, int start, int length ) throws Exception {
		super( state, address, port, "F"+function, new byte[12], new byte[ inboundTotalLength(function,length) ], transactionId );
		initBytes( transactionId, unitId, function, start );
		outbound.writeShortBE( (short)6, 4, 2 ); // length of request
		outbound.writeShortBE( (short)length, 10, 2 ); // length of data to receive back
		outboundMemoryValid();
	}

	// write request
	public OutboundModbusTCP ( ServerState state, String address, int port, int transactionId, int unitId, int function, int start, int[] writeValues ) throws Exception {
		super( state, address, port, "F"+function, new byte[12+writeValues.length], new byte[ inboundTotalLength(function,writeValues.length) ], transactionId );
		initBytes( transactionId, unitId, function, start );
		outbound.writeShortBE( (short)(6+writeValues.length), 4, 2 );
		for (int i=0; i<writeValues.length; i++) {
			outbound.writeShortBE( (short)writeValues[i], 12+(i*2), 2 );
		}
		outboundMemoryValid();
	}

	public Bytes inboundBytes (  ) {
		return inbound;
	}

	public Bytes outboundBytes (  ) {
		return outbound;
	}
	
	public static void main (String[] args) throws Exception {
	
		ServerState state = new ServerState();

		ServerTCP server0 = new ServerTCP( state, 9000, "server0", new byte[]{0x01,0x02,0x03}, 1024 );
		//ServerTCP server1 = new ServerTCP( state, 9001, "tcpserver1" , 16, 16 );
		
		while(server0.starting()) Thread.sleep(1);
		
		OutboundModbusTCP modbus = new OutboundModbusTCP(
			state,
			"localhost", // address
			9000, // port
			1,
			1,
			3, // function
			1, // starting register or coil
			1
		);
		System.out.println( "Outbound: "+modbus.outboundBytes() );
		modbus.capture(2);
		System.out.println( "Inbound:  "+modbus.inboundBytes() );
		
		Thread.sleep(1000);
		
		OutboundModbusTCP modbus1 = new OutboundModbusTCP(
			state,
			args[0], // address
			Integer.parseInt( args[1] ), // port
			1,
			1,
			Integer.parseInt( args[2] ), // function
			Integer.parseInt( args[3] ), // starting register or coil
			1
		);
		System.out.println( "Outbound 1: "+modbus.outboundBytes() );
		modbus.capture(2);
		System.out.println( "Inbound 1:  "+modbus.inboundBytes() );
	}

}
