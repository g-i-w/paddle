package paddle;

import java.util.*;

public class QueryString {

	private Map<String,String> map;
	private boolean error = false;

	public QueryString ( String query ) {
		map = new HashMap<>();
		try {
			String[] tuples = query.split("&");
			for (String tupleStr : tuples ) {
				try {
					String[] tuple = tupleStr.split("=");
					String key = tuple[0];
					String value;
					try {
						value = tuple[1];
					} catch (Exception e3) {
						value = "";
					}
					map.put(key, value);
				} catch (Exception e2) {
					error = true;
				}
			}
		} catch (Exception e1) {
			error = true;
		}
	}
	
	public Set<String> keys () {
		return map.keySet();
	}
	
	public String get ( String key, String defaultStr ) {
		if ( ! map.containsKey( key ) ) return defaultStr;
		return map.get(key);
	}
	
	public String get ( String key ) {
		return get( key, "" );
	}
	
	public boolean has ( String key ) {
		return map.containsKey( key );
	}
	
	public boolean has ( String key, String value ) {
		if (
			map.containsKey( key ) &&
			map.get( key ).toLowerCase().equals( value.toLowerCase() )
		) return true;
		return false;
	}
	
	public int getInt ( String key, int defaultInt ) {
		try {
			return Integer.parseInt( get( key ) );
		} catch (Exception e) {
			return defaultInt;
		}
	}
	
	public int getInt ( String key ) {
		return getInt( key, 0 );
	}
	
	public double getDouble ( String key, double defaultDouble ) {
		try {
			return Double.parseDouble( get( key ) );
		} catch (Exception e) {
			return defaultDouble;
		}
	}
	
	public double getDouble ( String key ) {
		return getDouble( key, 0.0 );
	}
	
	public String[] getCSV ( String key ) {
		try {
			return get( key ).split(",");
		} catch (Exception e) {
			return new String[0];
		}
	}
	
	public String toString () {
		return map.toString();
	}
	
	public boolean error () {
		return error;
	}
	
	public static void main ( String[] args ) {
		QueryString qs = new QueryString( "this=that&creates_key_anyway&two=2&4.4=4.4&=zero_len_key&1=1&zero_len_value=&normalCSV=a,b,c&singleCSV=only_one" );
		System.out.println(
			"has(\"this\",\"that\"): "+qs.has("this","that")+"\n"+
			"(getInt(\"two\"))*2: "+qs.getInt("two")*2+"\n"+
			"(getDouble(\"4.4\"))*2: "+qs.getDouble("4.4")*2+"\n"+
			"has [doesn't have]: "+qs.has("doesn't")+"\n"+
			"get [doesn't have]: '"+qs.get("doesn't")+"'\n"+
			"get [doesn't have], instead: '"+qs.get("doesn't","instead")+"'\n"+
			"CSV test: "+String.join(" ",qs.getCSV("normalCSV"))+"\n"+
			"CSV single test: "+String.join(" ",qs.getCSV("singleCSV"))+"\n"+
			"error(): "+qs.error()+"\n"+
			qs.toString()+"\n"+
			qs.keys()
		);
	}
}
