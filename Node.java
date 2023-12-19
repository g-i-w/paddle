package paddle;

public interface Node {

	public Connection send ( String remoteAddress, int remotePort, byte[] outboundData ) throws Exception;
	
	public Connection reply ( Connection c, byte[] outboundData ) throws Exception;
	
	public Connection forward ( Connection c, String remoteAddress, int remotePort ) throws Exception;
	
}
