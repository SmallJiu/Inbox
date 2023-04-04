package cat.jiu.sql.select;

import java.util.StringJoiner;

import cat.jiu.sql.SQLLogicOperator;
import cat.jiu.sql.SQLOperator;

public class Where implements ISQLWhere {
	protected final String id;
	protected final SQLOperator operator;
	protected final String wildcard;
	protected final SQLLogicOperator nextOperator;
	
	/**
	 * {@code new Where("id", SQLOperator.GREATER_EQUAL, "?", null)} will like 'id>=?'
	 * @param id the sql value id
	 * @param operator the operator
	 * @param nextOperator the next where logic operator, if this is last where, can be null
	 */
	public Where(String id, SQLOperator operator, String wildcard, SQLLogicOperator nextOperator) {
		this.id = id;
		this.operator = operator;
		this.wildcard = this.operator == SQLOperator.IS_NULL ? "" : wildcard;
		this.nextOperator = nextOperator;
	}
	
	public String getId() {return id;}
	public SQLOperator getOperator() {return operator;}
	public SQLLogicOperator getNextOperator() {return nextOperator;}
	
	public String toSQL() {
		StringJoiner sb = new StringJoiner(" ");
		
		sb.add(this.id);
		sb.add(this.operator.sql);
		
		if(this.operator == SQLOperator.LIKE
		|| this.operator == SQLOperator.NOT_LIKE) {
			sb.add(new StringBuilder()
					.append("'")
					.append(this.wildcard)
					.append("'"));
		}else {
			if(this.operator != SQLOperator.IS_NULL
			|| this.operator != SQLOperator.EXISTS
			|| this.operator != SQLOperator.NOT_EXISTS) sb.add(this.wildcard);
		}
		
		return sb.toString();
	}
}
