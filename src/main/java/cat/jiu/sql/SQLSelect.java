package cat.jiu.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

import cat.jiu.sql.select.ISQLWhere;

public class SQLSelect {
	public final SQLSelectType type;
	protected final ArrayList<ISQLWhere> wheres = new ArrayList<>();
	protected final Object[] args;
	/**
	 * @param whereArgs 预处理时需替换模糊匹配的值
	 */
	public SQLSelect(SQLSelectType type, Object... whereArgs) {
		this.type = type;
		this.args = whereArgs;
	}
	
	public SQLSelect add(ISQLWhere where) {
		this.wheres.add(where);
		return this;
	}
	public SQLSelect remove(int index) {
		this.wheres.remove(index);
		return this;
	}
	public SQLSelect set(int index, ISQLWhere where) {
		this.wheres.set(index, where);
		return this;
	}
	public Object[] getArgs() {
		return Arrays.copyOf(this.args, this.args.length);
	}
	
	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(" ");
		ISQLWhere pre = null;
		for(int i = 0; i < this.wheres.size(); i++) {
			if(pre!=null) {
				sj.add(pre.getNextOperator().name().toLowerCase());
			}
			ISQLWhere where = this.wheres.get(i);
			sj.add(where.toSQL());
			pre = where;
		}
		return sj.toString();
	}
}
