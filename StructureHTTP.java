package paddle;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class StructureHTTP implements HTTP {

	private ConcurrentMap<String,String> header = new ConcurrentHashMap<>();

	
	//////////////////// abstract method ////////////////////
	public abstract byte[] data ();

	//////////////////// abstract method ////////////////////
	public abstract int dataLength ();
	
	//////////////////// abstract method ////////////////////
	public abstract boolean complete ();
	
	//////////////////// abstract method ////////////////////
	public abstract void parse ( int validLength ) throws Exception;
	
	// HTTP interface
	public String method () { return null; }
	public String path () { return null; }
	
	public String code () { return null; }
	public String message () { return null; }

	
	public String protocol () {
		return "HTTP/1.1";
	}
	
	public String header ( String key ) {
		return header.getOrDefault( key, "" );
	}
	
	public void header ( String key, String value ) {
		header.put( key, value );
	}
	
	public void header ( String[] perlHash ) {
		if (perlHash==null) return;
		for (int i=0; i+1<perlHash.length; i+=2) {
			header( perlHash[i], perlHash[i+1] );
		}
	}
	
	public Map<String,String> header () {
		return header;
	}
	
	public String headerString () {
		StringBuilder headerString = new StringBuilder();
		for (Map.Entry<String,String> entry : header.entrySet()) {
			headerString
				.append(entry.getKey())
				.append(": ")
				.append(entry.getValue())
				.append("\r\n")
				;
		}
		return headerString.toString();
	}
	
	public int contentLength () {
		if (header.containsKey("Content-Length")) {
			try {
				return Integer.parseInt( header.get("Content-Length") );
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}
		return 0;
	}
	
	public byte[] document ( String firstLine, byte[] tempData ) {
		int tempDataLength = ( tempData==null ? 0 : tempData.length );
		byte[] firstBytes = ( firstLine+"\r\n"+headerString()+"\r\n" ).getBytes();
		byte[] allBytes = new byte[ firstBytes.length + tempDataLength ];
		for (int i=0; i<firstBytes.length; i++) {
			allBytes[ i ] = firstBytes[ i ];
		}
		for (int i=0; i<tempDataLength; i++) {
			allBytes[ i+firstBytes.length ] = tempData[ i ];
		}
		return allBytes;
	}
	
	public String toString () {
		return new String( document("", data()) );
	}
	
	// friendly methods for child classes
	
	boolean subSequence ( byte[] seq, byte[] mem, int start ) {
		if (start>=0 && start+seq.length-1<mem.length) {
			for (int i=0; i<seq.length; i++) {
				if (mem[start+i] != seq[i]) return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	boolean appendChar ( StringBuilder str, byte[] memory, int index ) {
		if (str!=null && memory!=null && index<memory.length) {
			char c = (char)(memory[index]&0xff); // &0xff may be required to eliminate sign-extension
			str.append( c );
			//System.out.println( str+" '"+c+"'" );
			return true;
		} else {
			return false;
		}
	}
}


class TestStructureHTTP extends StructureHTTP {

	public byte[] data () {
		return new byte[]{ '1', '2', '3' };
	}

	public int dataLength () {
		return data().length;
	}

	public void parse ( int validLength ) throws Exception {
	}
	
	public boolean complete () {
		return true;
	}
	
	public byte[] document () {
		return document( "GET / HTTP/1.1", data());
	}
	
	// testing
	public static void main ( String[] args ) {
		TestStructureHTTP http = new TestStructureHTTP();
		System.out.print(">");
		System.out.print(new String(http.document( "GET / HTTP/1.0", new byte[]{ 'a', 'b', 'c' })));
		System.out.print("<\n\n>");
		System.out.print("toString():\n>");
		System.out.print(http);
		System.out.print("<\n\n");
				
		TestStructureHTTP http1 = new TestStructureHTTP();
		http1.header( "Content-Type", "application/json" );
		System.out.print(">");
		System.out.print("toString():\n>");
		System.out.print(http1);
		System.out.print("<\n\n");
		
		System.out.println(
			"\n\n"+http1.subSequence(
				new byte[]{ 'G','E','T' },
				new byte[]{ 0,0,'G','E','T',0,0 },
				2
			)
		);
	}
}
