package paddle;

import java.io.*;

public class ReadStream extends Thread {
	
	private InputStream input;
	private boolean done;
	private Bytes bytesObject;
	private String text;
	private int length;
	
	public ReadStream ( InputStream input, String name ) {
		setName( name );
		this.input = input;
		bytesObject = new Bytes( 1024 );
		length = 0;
		done = false;
		start();
	}
	
	public void run () {
		try {
			int intContainingByte;
			while ( (intContainingByte = input.read()) > 0 ) { // not 0 (null) or -1 (end of stream)
				bytesObject.write( (byte)intContainingByte, length++ );
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
		if (bytesObject != null) {
			byte[] nonNull = new byte[length];
			for (int i=0; i<length; i++) {
				nonNull[i] = bytesObject.bytes()[i];
			}
			return nonNull;
			//return bytesObject.bytes();
		}
		else return new byte[0];
	}
	
	public boolean done () {
		return done;
	}
	
	public InputStream input () {
		return input;
	}
	
	public String toString () {
		return this.getClass().getName()+" '"+getName()+"': "+text();
	}
	
}

