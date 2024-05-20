package cat.jiu.sql;

public enum SQLOperator {
	EQUAL("="),
	IS("is"),
	IS_NULL("is null"),
	
	GREATER(">"),
	GREATER_EQUAL(">="),
	NOT_GREATER("!>"),
	
	LESS("<"),
	LESS_EQUAL("<="),
	NOT_LESS("!<"),
	NOT_EQUAL("!="),
	
	BETWEEN("between"),
	NOT_BETWEEN("not between"),
	
	LIKE("like"),
	NOT_LIKE("not like"),
	
	GLOB("glob"),
	NOT_GLOB("not glob"),
	
	IN("in"),
	NOT_INT("not in"),
	
	EXISTS("exists"),
	NOT_EXISTS("not exists");
	
	public final String sql;
	private SQLOperator(String sql) {
		this.sql = sql;
	}
}
