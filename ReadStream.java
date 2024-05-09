package paddle;

import java.io.*;

public class ReadStream extends Thread {
	
	private InputStream input;
	private boolean done;
	private boolean lastLineOnly;
	private Bytes bytesObject;
	private String text;
	private int cursor;
	
	public ReadStream ( InputStream input, String name ) {
		this( input, name, false );
	}
	
	public ReadStream ( InputStream input, String name, boolean lastLineOnly ) {
		setName( name );
		this.input = input;
		bytesObject = new Bytes();
		cursor = 0;
		done = false;
		this.lastLineOnly = lastLineOnly;
		start();
	}
	
	public void run () {
		try {
			int intContainingByte;
			while ( !done && (intContainingByte = input.read()) > 0 ) { // not 0 (null) or -1 (end of stream)
				if (lastLineOnly && (intContainingByte == '\n' || intContainingByte == '\r')) {
					cursor=0;
				} else {
					bytesObject.write( (byte)intContainingByte, cursor++ );
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		done = true;
	}
	
	public String text () {
		return text( "UTF-8" );
	}
	
	public String text ( String charsetName ) {
		try {
			return new String( bytes(), charsetName );
		} catch (Exception e) {
			return "";
		}
	}
	
	public byte[] bytes () {
		/*if (bytesObject != null) {
			int length = bytesObject.last()+1;
			byte[] nonNull = new byte[length];
			for (int i=0; i<length; i++) {
				nonNull[i] = bytesObject.bytes()[i];
			}
			return nonNull;
			//return bytesObject.bytes();
		}
		else return new byte[0];*/
		return bytesObject.data();
	}
	
	public boolean done () {
		return done;
	}
	
	public void end () {
		done = true;
	}
	
	public InputStream input () {
		return input;
	}
	
	public String toString () {
		return this.getClass().getName()+" '"+getName()+"': "+text();
	}
	
}

