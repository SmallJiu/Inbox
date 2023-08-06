package cat.jiu.sql.select;

import java.util.StringJoiner;

import cat.jiu.sql.SQLSelect;
import cat.jiu.sql.SQLSelectType;

public class InnerSelect extends SQLSelect {
	protected final String table;
	protected final String[] keys;
	public InnerSelect(SQLSelectType type, String table, String[] keys) {
		super(type);
		this.table = table;
		this.keys = keys;
	}
	public InnerSelect add(ISQLWhere where) {
		this.wheres.add(where);
		return this;
	}
	public InnerSelect remove(int index) {
		this.wheres.remove(index);
		return this;
	}
	public InnerSelect set(int index, ISQLWhere where) {
		this.wheres.set(index, where);
		return this;
	}
	public String toString() {
		StringJoiner key = new StringJoiner(",");
		for(int i = 0; i<this.keys.length; i++){
			key.add(this.keys[i]);
		}
		return "(select "+ key + " from " + this.table + " where " + super.toString() + ")";
	}
}
