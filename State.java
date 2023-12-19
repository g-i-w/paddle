package paddle;

import java.util.concurrent.atomic.AtomicBoolean;

public class State {

	private AtomicBoolean transitionLock = new AtomicBoolean(false);
	
	public void process ( Connection c ) {
		System.out.println(
			"\n***\n"+
			"Protocol:      "+c.protocol()+"\n"+
			"Direction:     "+(c.inbound() ? "IN" : "OUT")+"("+(c.complete() ? "complete" : "incomplete")+")\n"+
			"Server Name:   "+c.server().name()+"\n"+
			"Connection ID: "+c.connectionId()+"\n"+
			"Remote Addr:   "+c.remoteAddress()+":"+c.remotePort()+"\n"+
			"Local Addr:    "+c.localAddress()+":"+c.localPort()+"\n"+
			"Outbound Data: "+(c.outboundData() != null ? new String(c.outboundData()) : "")+"\n"+
			"Inbound Data:  "+(c.inboundData() != null ? new String(c.inboundData()) : "")+"\n"+
			"***\n"
		);
	}
	
	public void handle ( Connection c ) {
		while (transitionLock.compareAndSet( false, true )) {
			try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
		}
		process( c );
		transitionLock.set( false );
	}

	// respond to specific types of Connections
	// everything else is handled by generic respond( Connection ) in parent class
	
	public void respond ( ConnectionTCP c ) {
		handle( c );
	}
	
	
	public void respond ( OutboundUDP c ) {
		handle( c );
	}
	
	public void respond ( InboundHTTP session ) {
		session.response().setBody(
			"<h1>HTTP works!</h1>\n<br>\n"+
			"path: "+session.request().path()+"\n<br>\n"+
			"body: "+session.request().body()+"\n<br>"
		);
		handle( session );
	}
	

	public void respond ( InboundUDP rxPacket ) {
		try {
			Thread.sleep(100);
			OutboundUDP txPacket = rxPacket.reply(
				"UDP works! Received data ["+rxPacket.connectionId()+"]: '"+(new String(rxPacket.inboundData())+"'")
			);
			handle( txPacket );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
