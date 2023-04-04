package cat.jiu.sql;

public enum SQLJoinType {
	INNER, LEFT, RIGHT, FULL;
	
	public final String sql;
	private SQLJoinType() {
		this.sql = name() + " JOIN";
	}
}
