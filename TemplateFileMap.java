package paddle;

import java.io.*;
import java.nio.file.*;

public class TemplateFileMap extends FileMap<TemplateFile> {

	private String delim;

	public TemplateFile read ( File file ) throws IOException {
		return new TemplateFile( file.getPath(), delim );
	}
	
	public TemplateFileMap ( String path, String delim ) throws Exception {
		super( path );
		this.delim = delim;
	}

	public static void main ( String[] args ) {
		try {
			System.out.println( "files:\n"+(new TemplateFileMap( args[0], args[1] ) ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
}

