package paddle;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
	
	public boolean httpQuery ( InboundHTTP session, String key ) {
		if (session!=null) {
			Map<String,String> map = session.request().query();
			return map.get( key )!=null && !map.get( key ).equals("");
		}
		return false;
	}
	
	public boolean httpQuery ( InboundHTTP session, String key, String value ) {
		if (httpQuery( session, key )) return session.request().query().get( key ).equals( value );
		return false;
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
	
	///////////////////////	Deprecated! Retained here for compatability ///////////////////////
	public void respond ( Connection c ) {
		received( c );
	}

}
