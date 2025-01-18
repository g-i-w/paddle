package paddle;

import java.net.*;

public class ResponseHTTP extends StructureHTTP {

	public enum State {
		PROTOCOL,
		CODE,
		MESSAGE,
		HEADER_OR_RET,
		HEADER_VALUE,
		DATA,
		COMPLETE
	}

	private byte[] memory;
	private int index;
	
	private StringBuilder code;
	private StringBuilder message;
	private StringBuilder headerKey;
	private StringBuilder headerVal;
	private int dataStart = 0; // inclusive
	private int dataEnd = 0;   // exclusive
	

	private State state = State.PROTOCOL;
	
	
	
	// Incoming: stream -> structure
	public ResponseHTTP ( byte[] memory ) {
		this( memory, false );
	}

	public ResponseHTTP ( byte[] memory, boolean parseAll ) {
		this.memory = memory;
		code = new StringBuilder();
		message = new StringBuilder();
		headerKey = new StringBuilder();
		headerVal = new StringBuilder();
		if (parseAll) try {
			parse( memory.length );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Outgoing: structure -> stream
	public ResponseHTTP ( HTTP http ) {
		this( "200", "OK", null, http.data() );
	}
	
	public ResponseHTTP ( String data ) {
		this( "200", "OK", null, data.getBytes() );
	}
	
	public ResponseHTTP ( String[] header, byte[] data ) {
		this( "200", "OK", header, data );
	}

	public ResponseHTTP ( String[] header, String data ) {
		this( "200", "OK", header, data.getBytes() );
	}

	public ResponseHTTP ( String code, String message, String[] header, byte[] data ) {
		memory = data;
		if (memory!=null) dataEnd = memory.length;
		header( header );
		state = State.COMPLETE;
		this.code = new StringBuilder();
		this.message = new StringBuilder();
		this.code.append( code );
		this.message.append( message );
	}
	
	

	private boolean DIGIT () {
		return (memory[index] >= '0' && memory[index] <= '9');
	}
	
	private boolean SPACE () {
		return (memory[index]==' ' && index>0 && memory[index-1]!=' ');
	}
	
	private boolean EOL () {
		if (subSequence( new byte[]{'\r','\n'}, memory, index )) {
			index++;
			return true;
		} else return false;
	}
	
	private boolean EOD () {
		return (dataLength() >= contentLength()-1);
	}
	
	private boolean DELIM () {
		if (subSequence( new byte[]{':',' '}, memory, index )) {
			index++;
			return true;
		} else return false;
	}
	
	private boolean DATA () {
		if (subSequence( new byte[]{'\r','\n','\r','\n'}, memory, index )) {
			index += 4;
			return true;
		} else return false;
	}
	
	public String code () {
		return code.toString();
	}
	
	public String message () {
		return message.toString();
	}
	
	public byte[] data () {
		if (memory==null) return new byte[]{};
		int len = dataEnd - dataStart;
		byte[] currentData = new byte[len];
		for (int i=0; i<len; i++) {
			currentData[i] = memory[dataStart+i];
		}
		return currentData;
	}
	
	public byte[] memory () {
		return memory;
	}
	
	public void memory ( byte[] mem) {
		memory = mem;
	}
	
	public int dataLength () {
		return dataEnd - dataStart;
	}
	
	public boolean complete () {
		return (state == State.COMPLETE);
	}
	
	public void parse ( int validLength ) throws Exception {
		while (index < validLength && index < memory.length) {
			switch (state) {
				case PROTOCOL:
					if	(SPACE())	state = State.CODE;
					else if	(EOL())		throw new Exception( "Unexpected EOL" );
					break;
				case CODE:
					if	(SPACE())	state = State.MESSAGE;
					else if	(EOL())		throw new Exception( "Unexpected EOL" );
					else if (DIGIT())	appendChar( code, memory, index );
					break;
				case MESSAGE:
					if	(EOL())		state = State.HEADER_OR_RET;
					else			appendChar( message, memory, index );
					break;
				case HEADER_OR_RET:
					if (headerKey==null)	headerKey = new StringBuilder();
					if	(EOL())		{
									state = State.DATA;
									dataStart=index+1;
									dataEnd=dataStart;
								}
					else if (DELIM())	state = State.HEADER_VALUE;
					else			appendChar( headerKey, memory, index );
					break;
				case HEADER_VALUE:
					if (headerVal==null)	headerVal = new StringBuilder();
					if	(EOL())		{
									state = State.HEADER_OR_RET;
									header( headerKey.toString(), headerVal.toString() );
									headerKey = null;
									headerVal = null;
								}
					else			appendChar( headerVal, memory, index );
					break;
				case DATA:
					if      (EOD())		state = State.COMPLETE;
					else			dataEnd++;
					break;
				case COMPLETE:
					break;
				default :
					throw new Exception( "Abnormal state: '"+state.name()+"'" );
			}
			index++;
			//System.out.println( state.name()+header() );
			if (state == State.COMPLETE) break;
		}
	}
	
	public byte[] document () {
		header( "Content-Length", String.valueOf( dataLength() ) );
		return document(
			protocol()+" "+code()+" "+message(),
			data()
		);
	}
	
	public String toString () {
		return new String( document() );
	}
	
	
	public static void main ( String[] args ) {
	
		String simpleTest = "HTTP/12.0 200 OK\r\nHeader-Key: header_value\r\n";
		String simpleTestNoHeader = "HTTP/1.0 200 OK";
		
		String dataTest = "HTTP/1.0   404   Forbidden\r\nContent-Length: 4\r\nSomething: else\r\n\r\nabcd";
		String dataTestNoLength = "  HTTP/1.0     222  - - - ???\r\n\r\nabcd";
		
		System.out.println(
			new ResponseHTTP( simpleTest.getBytes(), true )
		);
		System.out.println(
			new ResponseHTTP( simpleTestNoHeader.getBytes(), true )
		);
		System.out.println(
			">"+(new ResponseHTTP( dataTest.getBytes(), true ))+"<"
		);
		System.out.println(
			">"+(new ResponseHTTP( dataTestNoLength.getBytes(), true ))+"<"
		);
		System.out.println(
			new ResponseHTTP( "this won't be parsed" )
		);
	}

}
