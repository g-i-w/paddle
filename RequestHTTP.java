package paddle;

import java.net.*;
import java.util.*;

public class RequestHTTP extends StructureHTTP {

	public enum State {
		FIND_CMD,
		GET_PATH,
		GET_DATA,
		POST_PATH,
		HEADER_OR_RET,
		HEADER_VALUE,
		POST_DATA,
		COMPLETE
	}

	private byte[] memory;
	private int index;
	
	private StringBuilder method;
	private boolean isGet;
	private StringBuilder path;
	private StringBuilder headerKey;
	private StringBuilder headerVal;
	private int dataStart = 0; // inclusive
	private int dataEnd = 0;   // exclusive
	

	private State state = State.FIND_CMD;
	

	// Incoming: stream -> structure
	public RequestHTTP ( byte[] memory ) {
		this( memory, false );
	}
	
	public RequestHTTP ( byte[] memory, String method, String path ) {
		this( memory, false );
		this.method.append( method );
		this.path.append( path );
	}

	public RequestHTTP ( byte[] memory, boolean parseAll ) {
		this.memory = memory;
		method = new StringBuilder();
		path = new StringBuilder();
		headerKey = new StringBuilder();
		headerVal = new StringBuilder();
		if (parseAll) try {
			parse( memory.length );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Outgoing: structure -> stream
	public RequestHTTP ( HTTP http ) {
		this( "POST", "/", null, http.data() );
	}
	
	public RequestHTTP ( String path, HTTP http ) {
		this( "POST", path, null, http.data() );
	}
	
	public RequestHTTP ( String method, String path, HTTP http ) {
		this( method, path, null, http.data() );
	}
	
	public RequestHTTP () {
		this( "GET", "/", null, null );
	}
	
	public RequestHTTP ( String path ) {
		this( "GET", path, null, null );
	}
	
	public RequestHTTP ( String path, byte[] data ) {
		this( "POST", path, null, data ); 
	}
	
	public RequestHTTP ( String method, String path, String[] header, byte[] data ) {
		memory = data;
		if (memory!=null) dataEnd = memory.length;
		header( header );
		if (method.equals("GET")) isGet = true;
		state = State.COMPLETE;
		this.method = new StringBuilder();
		this.path = new StringBuilder();
		this.method.append( method );
		this.path.append( path );
	}

	
		
	private boolean GET () {
		if (subSequence( new byte[]{'G','E','T'}, memory, index )) {
			index += 3;
			method.append( "GET" );
			isGet = true;
			return true;
		} else return false;
	}
	
	private boolean SPACE () {
		return (memory[index]==' ' && index>0 && memory[index-1]!=' ');
	}
	
	private boolean OKCHR () {
		return (memory[index]>=' ' && memory[index]<='~');
	}
	
	private boolean GET_DATA () {
		if (isGet && memory[index] == '?') {
			return true;
		} else return false;
	}
	
	private boolean PATH () {
		if (subSequence( new byte[]{' ','/'}, memory, index )) {
			return true;
		} else return false;
	}
	
	private boolean PROTOCOL () {
		if (subSequence( new byte[]{' ','H','T','T','P','/'}, memory, index )) {
			index += 10; // SP HTTP/1.1 CR LF
			return true;
		} else return false;
	}
	
	private boolean EOL () {
		if (subSequence( new byte[]{'\r','\n'}, memory, index )) {
			index++;
			return true;
		} else return false;
	}
	
	private boolean EOD () {
		return (dataLength() >= contentLength());
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
	
	public String method () {
		return method.toString();
	}
	
	public String path () {
		return path.toString();
	}
	
	public Map<String,String> query () {
		Map<String,String> query = new LinkedHashMap<>();
		try {
			String[] pairs = new String(data()).split("&");
			for (String pair : pairs) {
				String[] keyVal = pair.split("=");
				if (keyVal.length>1) query.put( keyVal[0], keyVal[1] );
				else if (keyVal.length>0) query.put( keyVal[0], "" );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query;
	}
	
	public byte[] data () {
		if (memory==null) return new byte[]{};
		int len = dataEnd - dataStart;
		byte[] currentData = new byte[len];
		for (int i=0; i<len; i++) {
			currentData[i] = memory[dataStart+i];
		}
		if (isGet) {
			try {
				return (URLDecoder.decode( new String(currentData), "UTF-8" )).getBytes();
			} catch (Exception e) {
				e.printStackTrace();
				return new byte[]{};
			}
		} else {
			return currentData;
		}
	}
	
	public int dataLength () {
		if (isGet) {
			return data().length;
		} else {
			return dataEnd - dataStart;
		}
	}
	
	public boolean complete () {
		return (state == State.COMPLETE);
	}
	
	public void parse ( int validLength ) throws Exception {
		while (index < validLength && index < memory.length) {
			switch (state) {
				case FIND_CMD:
					if	(GET())		state = State.GET_PATH;
					else if	(PATH())	state = State.POST_PATH; // for now, any method other than GET defaults to being treated as POST method
					else if	(index >= 15)	throw new Exception( "HTTP method not found in first 16 bytes" );
					else if (OKCHR())	appendChar( method, memory, index );
					break;
				case GET_PATH:
					if	(EOL())		throw new Exception( "Unexpected EOL" );
					else if	(GET_DATA())	{
									state = State.GET_DATA;
									dataStart = index+1;
									dataEnd = dataStart; // it's possible to have zero-length memory after '?' mark
								}
					else if (PROTOCOL())	state = State.HEADER_OR_RET;
					else if (OKCHR())	appendChar( path, memory, index );
					break;
				case POST_PATH:
					if	(PROTOCOL())	state = State.HEADER_OR_RET;
					else if	(EOL())		throw new Exception( "Unexpected EOL" );
					else if (OKCHR())	appendChar( path, memory, index );
					break;
				case GET_DATA:
					if	(PROTOCOL())	state = State.HEADER_OR_RET;
					else			dataEnd++;
					break;
				case HEADER_OR_RET:
					if (headerKey==null)	headerKey = new StringBuilder();
					if	(EOL())		{
									if (isGet) state = State.COMPLETE;
									else {
										state = State.POST_DATA;
										dataStart=index+1;
										dataEnd=dataStart;
									}
								}
					else if (DELIM())	state = State.HEADER_VALUE;
					else if (OKCHR())	appendChar( headerKey, memory, index );
					break;
				case HEADER_VALUE:
					if (headerVal==null)	headerVal = new StringBuilder();
					if	(EOL())		{
									state = State.HEADER_OR_RET;
									header( headerKey.toString(), headerVal.toString() );
									headerKey = null;
									headerVal = null;
								}
					else if (OKCHR())	appendChar( headerVal, memory, index );
					break;
				case POST_DATA:
					if (EOD() || isGet)	state = State.COMPLETE;
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
		//System.out.println( header );
		if (isGet) {
			String getData = "";
			byte[] data = data();
			if (data!=null && data.length>0) try {
				getData = "?"+URLEncoder.encode( new String( data ), "UTF-8" );
			} catch (Exception e) {
				e.printStackTrace();
			}
			return document(
				method+" "+path+getData+" "+protocol(),
				null
			);
		} else {
			header( "Content-Length", String.valueOf( dataLength() ) );
			return document(
				method+" "+path+" "+protocol(),
				data()
			);
		}
	}
	

	public String toString () {
		return new String( document() );
	}
	
	
	public static void main ( String[] args ) {
	
		String getTest = "GET /test/path/?some%20data HTTP/1.1\r\nHeader-Key: header_value\r\n";
		String getTestNoHeader = "GET /test/path/?some%20data HTTP/1.1";
		
		String postTest = "POST /test/path HTTP/1.1\r\nContent-Length: 4\r\nSomething: else\r\n\r\nabcd";
		String postTestNoLength = "PUT /test/path HTTP/1.2\r\n\r\nabcd";
		
		System.out.println(
			new RequestHTTP( getTest.getBytes(), true )
		);
		System.out.println(
			new RequestHTTP( getTestNoHeader.getBytes(), true )
		);
		System.out.println(
			">"+(new RequestHTTP( postTest.getBytes(), true ))+"<"
		);
		System.out.println(
			">"+(new RequestHTTP( postTestNoLength.getBytes(), true ))+"<"
		);
		
		HTTP req = new RequestHTTP( "POST", "/something", new String[]{ "a_key", "a_value" }, "request_data!".getBytes() );
		System.out.println(
			">"+req+"<"
		);
		System.out.println(
			">"+(new RequestHTTP( "GET", "/", req ))+"<"
		);
	
	}

}
