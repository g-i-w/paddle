package paddle;

import java.util.Map;

public interface HTTP {

	public String protocol (); // HTTP/1.1
	
	public String method (); // GET
	public String path (); // '/'
	
	public String code (); // 200
	public String message (); // OK
	
	public Map<String,String> header ();
	public String header ( String key );
	public void header ( String key, String value );
	
	public byte[] data ();
	public int dataLength ();
	
	public byte[] document ();
	
	public boolean complete ();
	
}
