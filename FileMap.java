package paddle;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public abstract class FileMap<T> extends HashMap<String,T> {

	public abstract T read ( File file ) throws IOException;
	

	public FileMap load ( File file ) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				for (File subFile : file.listFiles()) {
					if (!subFile.getName().equals("..") && !subFile.getName().equals(".")) load( subFile );
				}
			} else {
				put(
					file.getPath(),
					read( file )
				);
			}
		} else {
			throw new IOException( "unable to access '"+file+"'" );
		}
		return this;
	}
	
	public FileMap ( String path ) throws Exception {
		this( new File( path ) );
	}
	
	public FileMap ( File file ) throws Exception {
		load( file );
	}
	
}

