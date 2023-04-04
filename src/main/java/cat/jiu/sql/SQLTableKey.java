package cat.jiu.sql;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.StringJoiner;

public class SQLTableKey {
	private final LinkedHashMap<String, SQLKey> keys = new LinkedHashMap<>();
	
	public SQLTableKey put(String key, SQLKey type) {
		this.keys.put(key, type);
		return this;
	}
	public SQLTableKey remove(String key) {
		this.keys.remove(key);
		return this;
	}
	public SQLTableKey set(String key, SQLKey type) {
		this.keys.replace(key, type);
		return this;
	}
	public boolean has(String key) {
		return this.keys.containsKey(key);
	}
	public void clear() {
		this.keys.clear();
	}
	
	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(", ", "(", ")");
		for(Entry<String, SQLKey> value : this.keys.entrySet()) {
			sj.add(value.getKey() + " " + value.getValue().toString());
		}
		return sj.toString();
	}
}
