package paddle;

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
	
	
	
	///////////////////////	Deprecated! Retained here for compatability ///////////////////////
	public void respond ( Connection c ) {
		received( c );
	}

}
