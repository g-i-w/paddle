package paddle;

import java.io.*;
import java.nio.file.*;

public class BytesFileMap extends FileMap<Bytes> {

	public Bytes read ( File file ) throws IOException {
		return new Bytes(
			Files.readAllBytes( file.toPath() )
		);
	}
	
	public BytesFileMap ( String path ) throws Exception {
		super( path );
	}

	public static void main ( String[] args ) {
		try {
			System.out.println( "files:\n"+(new BytesFileMap( args[0] ) ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
}

