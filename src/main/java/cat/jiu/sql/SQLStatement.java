package cat.jiu.sql;

import java.sql.ResultSet;

public class SQLStatement {
	protected final SQLDatabase db;
	protected SQLStatement(SQLDatabase db) {
		this.db = db;
	}
	
	public int insert() {
		return 0;
	}
	public int delete() {
		return 0;
	}
	public int updata() {
		return 0;
	}
	public ResultSet select() {
		return null;
	}
	
	public boolean createTable() {
		return false;
	}
	public boolean createDatabase() {
		return false;
	}
	
	public boolean dropTable() {
		return false;
	}
	public boolean dropTableAllValues() {
		return false;
	}
	public boolean dropDatabase() {
		return false;
	}
	public boolean dropIndex() {
		return false;
	}
	
	public boolean execute() {
		return false;
	}
}
