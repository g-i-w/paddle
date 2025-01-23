package paddle;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;


public class ZipActions {

	public static void toFiles ( File outputDirectory, byte[] zipContent ) throws Exception {
		toFiles( outputDirectory, zipContent, false, false );
	}

	public static void toFiles ( File outputDirectory, byte[] zipContent, boolean forceOverwrite, boolean verbose ) throws Exception {
	// from https://stackoverflow.com/questions/23869228/how-to-read-file-from-zip-using-inputstream
	
		// create a buffer to improve copy performance later.
		byte[] buffer = new byte[2048];

		// open the zip stream
		ZipInputStream stream = new ZipInputStream( new ByteArrayInputStream( zipContent ) );

		try {
			// now iterate through each item in the stream. The get next
			// entry call will return a ZipEntry for each file in the
			// stream
			ZipEntry entry;
			while((entry = stream.getNextEntry())!=null) {
				
				// INPUT entry
				FileTime zipTime = entry.getLastModifiedTime();
				String zipName = entry.getName();
				if (verbose) System.out.print( "Entry: "+zipName+" ("+zipTime+")" );
				
				// OUTPUT file
				File outputFile = new File( outputDirectory, zipName );
				
				if (!forceOverwrite && outputFile.exists()) {
					FileTime fileTime = Files.getLastModifiedTime( outputFile.toPath() );
					if (verbose) System.out.print( " -> File: "+outputFile.getName()+" ("+fileTime+")" );
					if ( zipTime.compareTo(fileTime) <= 0 ) {
						if (verbose) System.out.println( " -> SKIPPED!" );
						continue; // SKIP file if zip time is older (negative) or same (==) as file
					}
				} else {
					// from: https://stackoverflow.com/questions/2833853/create-whole-path-automatically-when-writing-to-a-new-file
					// automatically create any necessary direcotries using the parent of outputFile (otherwise outputFile would also become a directory)
					outputFile.getParentFile().mkdirs();
				}

				// Once we get the entry from the stream, the stream is
				// positioned read to read the raw data, and we keep
				// reading until read returns 0 or less.
				FileOutputStream output = null;
				try {
					if (verbose) System.out.println( " -> Writing: "+zipName );
					output = new FileOutputStream( outputFile );
					int len = 0;
					while ((len = stream.read(buffer)) > 0) {
						output.write(buffer, 0, len);
					}
				}
				finally {
					// we must always close the output file
					if(output!=null) output.close();
				}
			}
		} finally {
			// we must always close the zip file.
			stream.close();
		}
	}
		
	public static void main ( String[] args ) throws Exception {
		File inputZip = new File( args[0] );
		File outputDir = new File( args[1] );
		toFiles(
			outputDir,
			Files.readAllBytes( inputZip.toPath() )
		);
	}
	
}
