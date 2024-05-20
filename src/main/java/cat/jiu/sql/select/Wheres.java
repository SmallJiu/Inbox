package cat.jiu.sql.select;

import java.util.ArrayList;
import java.util.StringJoiner;

import cat.jiu.sql.SQLLogicOperator;
import cat.jiu.sql.SQLOperator;

public class Wheres implements ISQLWhere {
	protected final SQLLogicOperator nextOperator;
	protected final ArrayList<ISQLWhere> wheres = new ArrayList<>();
	/**
	 * will like '(id=? and coin>=? or ((score<=? or balance>=?) and (name=? and nickname=?)))'
	 * @param nextOperator the next where logic operator, if this is last where, can be null
	 */
	public Wheres(SQLLogicOperator nextOperator) {
		this.nextOperator = nextOperator;
	}
	/**
	 * add sub to list
	 * @param where sub where
	 */
	public Wheres add(ISQLWhere where) {
		this.wheres.add(where);
		return this;
	}
	public Wheres remove(int index) {
		this.wheres.remove(index);
		return this;
	}
	public Wheres set(int index, ISQLWhere where) {
		this.wheres.set(index, where);
		return this;
	}
	
	public String getId() {return null;}
	public SQLOperator getOperator() {return null;}
	public SQLLogicOperator getNextOperator() {return nextOperator;}
	
	@Override
	public String toSQL() {
		StringJoiner sj = new StringJoiner(" ", "(", ")");
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
