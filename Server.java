package paddle;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Server extends Loop {

	// General
	private ServerState state;
	private int port;
	private AtomicInteger sequenceId = new AtomicInteger();
		
	
	public Server ( int port ) {
		this( new ServerState(), port, "paddle-service-port"+port );
	}

	public Server ( ServerState state, int port, String name ) {
		super( name );
		this.state 		= state;
		this.port 		= port;
		start();
	}
	
	public ServerState state () {
		return state;
	}
	
	public int port () {
		return port;
	}
	
	public String name () {
		return getName();
	}
	
	public int sequenceId () {
		return sequenceId.getAndIncrement();
	}
		
	/////////////// ABSTRACT METHOD ///////////////
	public abstract void init() throws Exception;

	/////////////// ABSTRACT METHOD ///////////////
	public abstract void loop () throws Exception;

	
	public String toString () {
		return
			super.toString()+
			"Port:         "+port+"\n"+
			"ServerState:  "+state+"\n"
		;
	}
	
}
