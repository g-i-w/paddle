package paddle;

import java.util.*;

public interface Connection {

	public String remoteAddress ();
	
	public int remotePort ();
	
	public String localAddress ();
	
	public int localPort ();
	
	public String protocol ();
	
	public boolean inbound ();
	
	public boolean complete ();
	
	public Server server ();
	
	public int connectionId ();
	
	public byte[] inboundData ();
	
	public byte[] outboundData ();
	
}
