package creek;

import java.util.*;

public class CSVLog extends CSVFile {

	private int indexCol;
	private Map<String,List<String>> index;
	
	public Log append ( Table table ) throws Exception {
		return this;
	}
	
	private CSV sliceToCSV ( Map<String,List<String>> slice ) {
		CSV csv = new CSV();
		for (List<String> row : slice) csv.append( row );
		return csv;
	}
	
	public CSVLog ( String path, int indexCol ) {
		this( new File( path ), indexCol );
	}
	
	public CSVLog ( File file, int indexCol ) {
		super( file );	
		this.indexCol = indexCol;
		index = new TreeMap<>();
	}
	
	
	public void write ( Table table, boolean append ) throws Exception {
		super.append( table, append );
		if (! append) index = new TreeMap<>();
		if (table != null) {
			for (List<String> row : table.data()) {
				if (row.size()>indexCol) index.put( row.get(indexCol), row );
			}
		}
	}
	
	public void clear () throws Exception {
		super.clear();
		index = new TreeMap<>();
	}




	// Log interface

	public Table last ( int lastRows ) {
		if (lastRows > rowCount()) lastRows = rowCount();
		return slice( rowCount()-lastRows, rowCount() );
	}
	
	public Table slice ( int startRowInclusive, int endRowExclusive ) {
		if (endRowExclusive > rowCount()) endRowExclusive = rowCount();
		CSV csv = new CSV();
		for (int i=startRowInclusive; i<endRowExclusive; i++) {
			csv.append( table().data().get(i) );
		}
	}
	
	public Table last ( String thisAndFollowing ) {
		if (index.size()==0 || thisAndFollowing==null) return null;
		Map<String,List<String>> slice = index.tailMap( thisAndFollowing );
		return sliceToCSV( slice );
	}
	
	public Table slice ( String thisAndFollowing, String approachingThisLimit ) {
		thisAndFollowing = index.ceilingKey( thisAndFollowing );
		approachingThisLimit = index.floorKey( approachingThisLimit );
		if (index.size()==0 || thisAndFollowing==null || approachingThisLimit==null) return null;
		Map<String,List<String>> slice = index.subMap( thisAndFollowing, true, approachingThisLimit, true );
		return sliceToCSV( slice );
	}
	
	public Log append ( Table table ) throws Exception {
		super.append( table );
	}

}
