package paddle;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.*;
import java.io.*;

public class ServerState {

	private AtomicBoolean transitionLock = new AtomicBoolean(false);
	
	public void print ( Connection c ) {
		System.out.println(
			"["+c.connectionId()+"] Protocol:      "+c.protocol()+" ("+(c.inbound() ? "inbound" : "outbound")+", "+(c.complete() ? "complete" : "active")+")\n"+
			"["+c.connectionId()+"] Remote Addr:   "+c.remoteAddress()+":"+c.remotePort()+"\n"+
			"["+c.connectionId()+"] Local Addr:    "+c.localAddress()+":"+c.localPort()+"\n"+
			"["+c.connectionId()+"] Outbound Data: "+(c.outboundData() != null ? new String(c.outboundData()) : "")+"\n"+
			"["+c.connectionId()+"] Inbound Data:  "+(c.inboundData() != null ? new String(c.inboundData()) : "")
		);
	}
	
	public void log ( String info, Connection c, boolean incoming ) {
		System.out.println(
			"["+c.connectionId()+"] "+
			info+" "+
			c.protocol()+" "+
			c.localAddress()+":"+c.localPort()+
			(incoming ? " <-- " : " --> ")+
			c.remoteAddress()+":"+c.remotePort()
		);
	}
	
	
	// Concurrency tools

	public void obtainLock () {
		while (transitionLock.compareAndSet( false, true )) {
			try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public void releaseLock () {
		transitionLock.set( false );
	}


	// Events: opened, received, closed

	public void opened ( Connection c ) {
		//log( "OPENED:", c, c.inbound() );
	}
	
	public void received ( Connection c ) {
		log( "RX "+(c.inboundData() != null ? c.inboundData().length+" bytes" : ""), c, true );
	}
	
	public void transmitted ( Connection c ) {
		log( "TX "+(c.outboundData() != null ? c.outboundData().length+" bytes" : ""), c, false );
	}
	
	public void closed ( Connection c ) {
		//log( "CLOSED:", c, c.inbound() );
	}
	
	
	// Protocol helpers
	
	public InboundUDP udp ( Connection c ) {
		if (c instanceof InboundUDP) return (InboundUDP)c;
		else return null;
	}
	
	public InboundTCP tcp ( Connection c ) {
		if (c instanceof InboundTCP) return (InboundTCP)c;
		else return null;
	}
	
	public InboundHTTP http ( Connection c ) {
		if (c instanceof InboundHTTP) return (InboundHTTP)c;
		else return null;
	}
	
	public boolean httpPath ( InboundHTTP session, String path ) {
		if (session!=null) return session.request().path().equals( path );
		return false;
	}
	
	public boolean httpPathBegins ( InboundHTTP session, String root ) {
		if (session==null) return false;
		int rootLen = root.length();
		String path = session.request().path();
		int pathLen = path.length();
		if (pathLen >= rootLen && path.substring( 0, rootLen ).equals( root )) return true;
		return false;
	}
	
	public boolean httpQuery ( InboundHTTP session, String key ) {
		if (session!=null) {
			Map<String,String> map = session.request().query();
			return map.containsKey( key );
		}
		return false;
	}
	
	public boolean httpQuery ( InboundHTTP session, String key, String value ) {
		if (httpQuery( session, key )) return session.request().query().get( key ).equals( value );
		return false;
	}
	
	public Map<String,String> getHttpQuery ( InboundHTTP session ) {
		if (session==null) return null;
		return session.request().query();
	}
	
	public String getHttpQuery ( InboundHTTP session, String key, String defaultValue ) {
		if (session==null) return null;
		String value = session.request().query().get( key );
		if (value != null & !value.equals("")) return value;
		return defaultValue;
	}
	
	public Map<String,String> httpQueryFields ( InboundHTTP session, String[] fields ) {
		Map<String,String> map1 = new TreeMap<>();
		if (session!=null) {
			Map<String,String> map0 = session.request().query();
			for (String field : fields) {
				if (map0.containsKey(field)) {
					String value = map0.get(field);
					if (value!=null) map1.put( field, value );
					else map1.put( field, "" );
				}
			}
		}
		return map1;
	}
	
	public ResponseHTTP httpFileResponse ( InboundHTTP session, String rootPath, String mimeType, long maxSize ) {
		String path = session.request().path();
		if (path.indexOf("..") == -1) { // block dir traversal
			try {
				// File object
				File file = new File( rootPath+path );
				// check for directory
				if (file.isDirectory()) throw new Exception( "ERROR: '"+file.getAbsolutePath()+"' is directory" );
				// Path object
				Path pathObj = file.toPath();
				// check size (-1 to disable check)
				long size = Files.size(pathObj);
				if (maxSize > -1 && size > maxSize) throw new Exception( "ERROR: file '"+file.getAbsolutePath()+"' size "+size+" greater than max "+maxSize );
				// read all bytes
				return new ResponseHTTP( new String[]{ "Content-Type", mimeType }, Files.readAllBytes( pathObj ) );
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseHTTP( "404", "Not Found", null, null );
			}
		} else {
			return new ResponseHTTP( "403", "Forbidden", null, null );
		}
	}
	
	///////////////////////	Deprecated! Retained here for compatability ///////////////////////
	public void respond ( Connection c ) {
		received( c );
	}

}
