package paddle;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ConnectionTCP extends Server implements Connection {

	private Socket 			socket;
	private int 			connectionId;
	
	private String 			remoteIP;
	private String			remoteDomain;
	private int 			remotePort;
	private String 			localAddress;
	private int 			localPort;
	
	protected InputStream 		input;
	protected OutputStream 		output;
	
	private byte[] 			inboundMemory;
	private int 			inboundMemoryPlace;
	private int 			chunks;

	private byte[] 			outboundMemory;
	private int 			outboundMemoryPlace;
	private int 			outboundMemoryValid;
	
	private int 			timeout;
	private long			timeoutStart;
	
	private boolean			complete = false;
	
	private ConnectionTCP		otherConnection;


	public ConnectionTCP (
		ServerState state,
		Socket socket,
		String name,
		byte[] outboundMemory,
		byte[] inboundMemory,
		int connectionId,
		int timeout,
		boolean allValid
	) {
		super( state, socket.getLocalPort(), name );
		this.socket 		= socket;
		this.connectionId 	= connectionId;
		this.inboundMemory 	= inboundMemory;
		inboundMemoryPlace 	= 0;
		this.outboundMemory 	= outboundMemory;
		outboundMemoryPlace 	= 0;
		outboundMemoryValid 	= ( allValid ? outboundMemory.length : 0 );
		chunks 			= 0;
		this.timeout 		= timeout; // ms
	}
	

	public void init () throws Exception {
		// timeout
		timeoutStart = System.currentTimeMillis();

		// pre-initialize in case of exception
		remoteIP 		= "";
		remoteDomain 		= "";
		remotePort 		= 0;
		localAddress 		= "";
		localPort 		= 0;

		// make sure the Socket object has been initialized 
		while (socket==null) {
			if ((int)(System.currentTimeMillis() - timeoutStart) > 10000) // can't use timeout value, because we're still in call to super( ) and it hasn't been initialized yet!
				throw new Exception( "ConnectionTCP: timeout while waiting for socket to become non-null" );
			sleep(1);
		}

		// local socket params
		localPort 			= socket.getLocalPort();
		String[] localAddressSplit	= socket.getLocalAddress().toString().split("\\/");
		localAddress 			= localAddressSplit[1];
		
		// remote socket params
		//String remoteAddressPort 	= socket.getRemoteSocketAddress().toString();
		String[] remoteAddressPortSplit	= socket.getRemoteSocketAddress().toString().split("[:\\/]");
		remoteDomain 			= remoteAddressPortSplit[0];
		remoteIP 			= remoteAddressPortSplit[1];
		remotePort 			= Integer.parseInt(remoteAddressPortSplit[2]);
		
		// input and output streams from socket
		//input 				= new BufferedInputStream( socket.getInputStream() );			
		input 				= socket.getInputStream();			
		output 				= socket.getOutputStream();

		// initial ServerState responce
		state().opened( this );
		//System.out.println( this.getClass().getName()+" socket: "+socket );
	}
	
	public void loop () throws Exception {
		boolean writing = false;
		boolean reading = false;

		// WRITE loop
		// catch up on writing if outboundMemory has increased...
		while (
			outboundMemory != null &&
			outboundMemory.length >= outboundMemoryValid &&
			outboundMemoryValid > outboundMemoryPlace
		) {
			int length = outboundMemoryValid - outboundMemoryPlace;
			output.write( outboundMemory, outboundMemoryPlace, length );
			//System.out.println( this.getClass().getName()+" sent: "+(char)outboundMemory[outboundMemoryPlace] );
			outboundMemoryPlace = outboundMemoryValid;
			writing = true;
		}
		output.flush();
		
		// READ loop
		// catch up on reading if there's data in the stream...
		int nextByte = 0;
		while (
			inboundMemory != null &&
			input.available() > 0 &&
			inboundMemory.length > inboundMemoryPlace &&
			(nextByte = input.read()) != -1 // might block, so check this last
		) {
			inboundMemory[inboundMemoryPlace] = (byte)nextByte;
			//System.out.println( this.getClass().getName()+" received: "+(char)inboundMemory[inboundMemoryPlace] );
			//System.out.print( (char)inboundMemory[inboundMemoryPlace] );
			inboundMemoryPlace++;
			reading = true;
		}
		
		// DATA flag or SLEEP

		// if we read something, increment chunks
		if (reading) {
			chunks++;
			received();
			if (inboundMemoryFull()) inboundMemoryFullEvent();
		}
		
		// if we wrote something
		if (writing) {
			transmitted();
		}
		
		// either we did something or sleep 1 ms
		if (writing || reading) {
			timeoutStart = System.currentTimeMillis();
		} else {
			sleep(10);
		}
		
		// check for client closing socket
		if (nextByte == -1) {
			end();
		}
		
		// check for timeout
		if ((int)(System.currentTimeMillis() - timeoutStart) > timeout) {
			end();
		}
	}
	
	public void end () {
		state().closed( this );
		super.end();
	}
	
	public void received () {
		state().received( this );
		synchronize();
	}
	
	public void transmitted () {
		state().transmitted( this );
	}
	
	public void loopEnded () {
		complete = true;
		//System.out.println( this.getClass().getName()+" loop ended." );
		if (socket != null) {
			try {
				socket.close();
				//System.out.println( this.getClass().getName()+" '"+getName()+"': socket now closed." );
			} catch (Exception e) {
				System.out.println( this.getClass().getName()+" '"+getName()+"': ERROR closing socket." );
				e.printStackTrace();
			}
		} else {
			//System.out.println( this.getClass().getName()+" '"+getName()+"': socket was already closed." );
		}
	}
	
	// specific to ConnectionTCP
	
	public byte[] outboundMemory () {
		return outboundMemory;
	}
	
	public byte[] outboundMemory ( byte[] newMemory ) {
		byte[] temp = outboundMemory;
		outboundMemoryValid = 0;
		outboundMemoryPlace = 0;
		outboundMemory = newMemory;
		return temp;
	}
	
	public int outboundMemoryPlace () {
		return outboundMemoryPlace;
	}
	
	// increase length of valid region
	public boolean outboundMemoryValid ( int setValid ) {
		if (setValid > outboundMemoryValid && setValid <= outboundMemory.length) {
			outboundMemoryValid = setValid;
			return true;
		}
		return false;
	}
	
	// all outbound memory is valid
	public void outboundMemoryValid () {
		outboundMemoryValid = outboundMemory.length;
	}
	
	// write to memory following valid data and return true if successful
	public boolean outboundMemoryWrite ( byte[] someBytes ) {
		//System.out.println( "outboundMemoryWrite: "+(new String(someBytes)) );
		if (outboundMemory.length - outboundMemoryValid < someBytes.length) return false;
		for (int i=0; i<someBytes.length; i++) {
			outboundMemory[outboundMemoryValid+i] = someBytes[i];
		}
		outboundMemoryValid += someBytes.length;
		return true;
	}
	
	public byte[] inboundMemory () {
		return inboundMemory;
	}
	
	protected byte[] inboundMemory ( byte[] next ) {
		byte[] prev = inboundMemory;
		inboundMemory = next;
		return prev;
	}
	
	protected void inboundMemory ( int size ) {
		byte[] next = new byte[size];
		byte[] prev = inboundMemory( next );
		int nextLen = next.length;
		int prevLen = prev.length;
		if (nextLen > prevLen) System.arraycopy( prev, 0, next, 0, prevLen );
		else System.arraycopy( prev, 0, next, 0, nextLen );
	}
	
	public int inboundMemoryPlace () {
		return inboundMemoryPlace;
	}
	
	protected void inboundMemoryPlace ( int place ) {
		inboundMemoryPlace = place;
	}
	
	protected void inboundMemoryFullEvent () {
		System.err.println( name()+":"+port()+" ("+getClass().getName()+") inbound memory full!" );
	}
	
	public boolean inboundMemoryFull () {
		return (inboundMemoryPlace >= inboundMemory.length);
	}
	
	public ConnectionTCP receive ( int chunksToReceive ) {
		return receiveAndClose(chunksToReceive, inboundMemory.length);
	}
	
	public ConnectionTCP receive () {
		return receive(1);
	}
	
	public ConnectionTCP capture ( int bytesToCapture ) {
		return receiveAndClose(bytesToCapture, bytesToCapture);
	}
	
	public ConnectionTCP capture () {
		return capture(inboundMemory.length);
	}
	
	public ConnectionTCP timeout ( int timeout ) {
		this.timeout = timeout;
		return this;
	}
	
	public int timeout () {
		return timeout;
	}
	
	// criteria for completion of the TCP connection
	
	public ConnectionTCP receiveAndClose ( int chunksToReceive, int bytesToCapture ) {
		while (
			chunks < chunksToReceive &&
			inboundMemoryPlace < bytesToCapture &&
			running()
		) wait(1);
		
		close();
		return this;
	}
	
	public ConnectionTCP sendAndClose () {
		return sendAndClose( outboundMemory.length );
	}
	
	public ConnectionTCP sendAndClose ( int bytesToSend ) {
		while (
			outboundMemoryPlace < bytesToSend &&
			outboundMemoryPlace < outboundMemory.length &&
			running()
		) wait(1);
		
		close();
		return this;
	}
	
	
	// misc
	
	public int chunks () {
		return chunks;
	}
	
	public String text () {
		return new String( inboundData() );
	}
	
	public String hex () {
		return ( new Bytes(inboundData()) ).toString();
	}
	
	public void close () {
		//System.out.println( this.getClass().getName()+" is closing..." );
		end();
	}
	
	public byte[] data ( byte[] memory, int memoryPlace ) {
		byte[] currentData = new byte[memoryPlace];
		for (int i=0; i<memoryPlace; i++) {
			currentData[i] = memory[i];
		}
		return currentData;
	}
	
	public void synchronize ( ConnectionTCP otherConnection ) {
		this.otherConnection = otherConnection;
		synchronize();
	}
	
	public void synchronize () {
		if (otherConnection != null) otherConnection.outboundMemoryValid( this.inboundMemoryPlace() );
	}
	
	public String remoteIP () {
		return remoteIP;
	}

	public String remoteDomain () {
		return remoteDomain;
	}

	// Connection interface
	
	public String remoteAddress () {
		if (starting()) return null;
		return ( remoteDomain.equals("") ? remoteIP : remoteDomain );
	}
	public int remotePort () {
		return remotePort;
	}
	public String localAddress () {
		return localAddress;
	}
	public int localPort () {
		return localPort;
	}
	
	public String protocol () {
		return "TCP";
	}
	
	///////////////// ABSTRACT /////////////////
	public abstract boolean inbound ();
	
	public boolean complete () {
		return complete;
	}
	
	///////////////// ABSTRACT /////////////////
	public abstract Server server ();
	
	public int connectionId () {
		return connectionId;
	}
	
	public byte[] inboundData () {
		return data( inboundMemory, inboundMemoryPlace );
	}
	
	public byte[] outboundData () {
		return data( outboundMemory, outboundMemoryPlace );
	}
	
}
