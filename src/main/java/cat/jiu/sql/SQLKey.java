package cat.jiu.sql;

import java.sql.JDBCType;
import java.util.StringJoiner;

public class SQLKey {
	protected final SQLDatabaseDrvier database;
	protected JDBCType type;
	protected Integer length;
	protected boolean isPrimaryKey;
	protected boolean isAutoIncrement;
	protected boolean isNotNull;
	protected Object default_value;
	
	protected SQLKey(SQLDatabaseDrvier database, JDBCType type) {
		this.type = type;
		this.database = database;
	}
	
	/**
	 * 设置是否为主键
	 */
	public SQLKey setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
		return this;
	}
	/**
	 * 设置字段长度
	 */
	public SQLKey setLength(int length) {
		this.length = length;
		return this;
	}
	/**
	 * 设置是否可以自增
	 */
	public SQLKey setAutoIncrement(boolean isAutoIncrement) {
		if(this.type == JDBCType.INTEGER)
			this.isAutoIncrement = isAutoIncrement;
		return this;
	}
	/**
	 * 设置是否为非空
	 */
	public SQLKey setNotNull(boolean isNotNull) {
		this.isNotNull = isNotNull;
		return this;
	}
	/**
	 * 设置默认数据
	 */
	public SQLKey setDefaultValue(Object value) {
		this.default_value = value;
		return this;
	}
	/**
	 * 设置字段数据类型
	 */
	public SQLKey setType(JDBCType type) {
		if(type != null) this.type = type;
		return this;
	}
	
	/**
	 * 生成SQL语句
	 * @return SQL 语句
	 */
	@Override
	public String toString() {
		StringJoiner sb = new StringJoiner(" ");
		
		StringBuilder type = new StringBuilder(this.type.getName());
		if(this.length!=null && !this.isAutoIncrement) {
			type.append("(")
				.append(this.length)
				.append(")");
		}
		sb.add(type);
		
		if(this.isPrimaryKey) 
			sb.add("primary key");
		if(this.isAutoIncrement) 
			sb.add(this.getAutoincrement());
		
		if(this.isNotNull) 
			sb.add("not null");
		if(this.default_value!=null) 
			sb.add("default");
			sb.add(String.valueOf(this.default_value));
		
		return sb.toString();
	}
	
	/**
	 * @return 从数据库类型获取自增字段的sql语句
	 */
	protected String getAutoincrement() {
		switch(this.database) {
			case MYSQL: return "auto_increment";
			case Microsoft_Access:
			case SQLite : return "autoincrement";
			case SQLServer: return "indentity";
			case Oracle: return "increment";
			case IBM_DB2: return "generated always as identity";
			case PostgreSQL: return "serial";
			default: return "";
		}
	}
}
