package paddle;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.*;
import java.io.*;
import static java.util.Map.entry; 

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
	
	
	// UDP connection
	
	public InboundUDP udp ( Connection c ) {
		if (c instanceof InboundUDP) return (InboundUDP)c;
		else return null;
	}
	
	// TCP connection
	
	public InboundTCP tcp ( Connection c ) {
		if (c instanceof InboundTCP) return (InboundTCP)c;
		else return null;
	}
	
	// HTTP connection
	
	public InboundHTTP http ( Connection c ) {
		if (c instanceof InboundHTTP) return (InboundHTTP)c;
		else return null;
	}
	
	// HTTP path
	
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
	
	// HTTP query
	
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
	
	// HTTP response object
	
	public ResponseHTTP httpStringResponse ( String content, String mimeType ) {
		return new ResponseHTTP(
			new String[]{ "Content-Type", mimeType },
			content
		);
	}
	
	public ResponseHTTP httpBinaryResponse ( byte[] bytes, String mimeType ) {
		return new ResponseHTTP(
			new String[]{ "Content-Type", mimeType },
			bytes
		);
	}
	
	// HTTP respond now
	
	public boolean httpRespond( InboundHTTP session, ResponseHTTP response ) {
		if (session != null) {
			session.response( response );
			return true;
		} else return false;
	}
	
	// HTTP code
	
	public boolean httpRespondCode ( InboundHTTP session, String code, String message ) {
		return httpRespond( session, new ResponseHTTP( code, message, null, null ) );
	}
	
	public boolean httpRespondBadRequest ( InboundHTTP session ) {
		return httpRespond( session, httpBadRequest() );
	}
	
	public boolean httpRespondForbidden ( InboundHTTP session ) {
		return httpRespond( session, httpForbidden() );
	}
	
	public boolean httpRespondNotFound ( InboundHTTP session ) {
		return httpRespond( session, httpNotFound() );
	}
		
	public ResponseHTTP httpBadRequest () {
		return new ResponseHTTP( "400", "Bad Request", null, null );
	}
	
	public ResponseHTTP httpForbidden () {
		return new ResponseHTTP( "403", "Forbidden", null, null );
	}
	
	public ResponseHTTP httpNotFound () {
		return new ResponseHTTP( "404", "Not Found", null, null );
	}
		
	// HTTP data
		
	public boolean httpRespondText ( InboundHTTP session, String text ) {
		return httpRespond( session, httpStringResponse( text, "text/plain" ) );
	}
		
	public boolean httpRespondHTML ( InboundHTTP session, String html ) {
		return httpRespond( session, httpStringResponse( html, "text/html" ) );
	}
		
	public boolean httpRespondJSON ( InboundHTTP session, String json ) {
		return httpRespond( session, httpStringResponse( json, "application/json" ) );
	}
		
	// HTTP file response
		
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
				//return new ResponseHTTP( new String[]{ "Content-Type", mimeType }, Files.readAllBytes( pathObj ) );
				return httpBinaryResponse( Files.readAllBytes( pathObj ), mimeType );
			} catch (Exception e) {
				e.printStackTrace();
				return httpNotFound();
			}
		} else {
			return httpForbidden();
		}
	}
	
	public String[] httpMIME () {
		return new String[] {
			".txt",		"text/plain",
			".htm",		"text/html",
			".html",	"text/html",
			".js",		"application/javascript",
			".json",	"application/json",
			".jpg",		"image/jpg",
			".jpeg",	"image/jpg",
			".png",		"image/png"
		};
	}
	
	public String httpMIME (  InboundHTTP session ) {
		String path = session.request().path();
		String ext = path.substring( path.lastIndexOf(".")+1, path.length() );
		String[] mimes = httpMIME();
		for (int i=1; i<mimes.length; i+=2) {
			if (ext.equals(mimes[i-1])) return mimes[i];
		}
		return null;
	}
	
	public ResponseHTTP httpFileResponse ( InboundHTTP session, String rootPath ) {
		String mime = httpMIME( session );
		if (mime != null) return httpFileResponse( session, rootPath, mime, -1 );
		else return httpForbidden();
	}
	
	public boolean httpRespondFile ( InboundHTTP session, String rootPath ) {
		return httpRespond( session, httpFileResponse( session, rootPath ) );
	}
	
	
	///////////////////////	Deprecated! Retained here for compatability ///////////////////////
	public void respond ( Connection c ) {
		received( c );
	}

}
