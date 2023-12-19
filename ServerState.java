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
		System.out.println( "["+c.connectionId()+"] OPENED" );
	}
	
	public void received ( Connection c ) {
		print( c );
	}
	
	public void transmitted ( Connection c ) {
		print( c );
	}
	
	public void closed ( Connection c ) {
		System.out.println( "["+c.connectionId()+"] CLOSED" );
	}
	
	
	
	///////////////////////	Deprecated! Retained here for compatability ///////////////////////
	public void respond ( Connection c ) {
		received( c );
	}

}
