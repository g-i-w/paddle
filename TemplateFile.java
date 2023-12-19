package paddle;

import java.util.*;
import java.nio.file.*;
import java.io.File;


public class TemplateFile {

	private List<String> fileFragments;
	private HashMap<String, String> replacements;
	
	public TemplateFile ( String filePath ) {
		this( filePath, null );
	}
	
	public TemplateFile ( String filePath, String delimiter ) {
		this( filePath, delimiter, "\n" );
	}

	public TemplateFile ( String filePath, String delimiter, String newLine ) {
		fileFragments = new ArrayList<>();
		replacements = new HashMap<>();
	
		try ( Scanner sc = new Scanner( new File( filePath ), "UTF-8" ) ) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (delimiter != null && line.indexOf( delimiter ) != -1) {
					String[] lineArray = line.split( delimiter );
					for (String fragment : lineArray) {
						fileFragments.add( fragment );
					}
					fileFragments.add( newLine );
				} else {
					fileFragments.add( line+newLine );
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TemplateFile replace ( String keyword, String replacement ) {
		replacements.put( keyword, replacement );
		return this;
	}
	
	public TemplateFile replace ( String[] reps ) {
		for (int i=0; i<reps.length; i+=2) {
			if (i+1 >= reps.length) break;
			replace( reps[i], reps[i+1] );
		}
		return this;
	}
	
	public TemplateFile revert () {
		replacements = new HashMap<>();
		return this;
	}
	
	public String toString () {
		String rendered = "";
		for (String fragment : fileFragments) {
			if (replacements.containsKey(fragment)) {
				rendered += replacements.get(fragment);
			} else {
				rendered += fragment;
			}
		}
		return rendered;
	}
	
}