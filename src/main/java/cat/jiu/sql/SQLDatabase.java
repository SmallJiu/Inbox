package cat.jiu.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import scala.actors.threadpool.Arrays;

public class SQLDatabase implements AutoCloseable {
	protected final Connection connection;
	protected final SQLDatabaseDrvier drvier;
	
	public final SQLPreparedStatement prepared;
	public final SQLStatement statement;
	
	/**
	 * 使用现有的连接对象创建
	 * @param connection 数据库的连接对象
	 */
	public SQLDatabase(Connection connection) {
		this.connection = connection;
		this.drvier = this.getDrvier(connection.getClass().getName());
		this.prepared = this.getSQLPreparedStatement();
		this.statement = this.getSQLStatement();
	}
	/**
	 * 使用一个地址创建
	 * @param url 数据库的地址，用于创建连接
	 * @throws SQLException 抛出自{@link DriverManager#getConnection(String)}
	 */
	public SQLDatabase(String url) throws SQLException {
		this(DriverManager.getConnection(url));
	}
	/**
	 * 使用一个地址并加上用户名与密码创建
	 * @param url 数据库的地址，用于创建连接
	 * @param username 连接数据库的用户名
	 * @param password 连接数据库的密码
	 * @throws SQLException 抛出自{@link DriverManager#getConnection(String, String, String)}
	 */
	public SQLDatabase(String url, String username, String password) throws SQLException {
		this(DriverManager.getConnection(url, username, password));
	}
	/**
	 * 使用一个地址并添加自定义信息创建
	 * @param url 数据库的地址，用于创建连接
	 * @param info 创建数据库时的自定义信息
	 * @throws SQLException 抛出自{@link DriverManager#getConnection(String, Properties)}
	 */
	public SQLDatabase(String url, Properties info) throws SQLException {
		this(DriverManager.getConnection(url, info));
	}
	
	/**
	 * 获取预处理对象，并对语句内的通配符进行替换
	 * @param sql 需要执行的sql语句
	 * @param whereArgs 通配符的替换
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement getPreparedStatement(String sql, Object[] whereArgs) throws SQLException {
		System.out.println(sql + ": " + Arrays.toString(whereArgs));
		PreparedStatement stmt = this.connection.prepareStatement(sql);
		for(int i = 1; whereArgs!=null && i < whereArgs.length+1; i++) {
			stmt.setString(i, String.valueOf(whereArgs[i-1]));
		}
		return stmt;
	}
	
	public PreparedStatement getPreparedStatement(String sql) throws SQLException {
		return this.connection.prepareStatement(sql);
	}
	
	public PreparedStatement getPreparedStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return this.connection.prepareStatement(sql, autoGeneratedKeys);
	}
	
	public PreparedStatement getPreparedStatement(String sql, int[] columnIndexes) throws SQLException {
		return this.connection.prepareStatement(sql, columnIndexes);
	}
	
	public PreparedStatement getPreparedStatement(String sql, String[] columnNames) throws SQLException {
		return this.connection.prepareStatement(sql, columnNames);
	}
	
	public PreparedStatement getPreparedStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}
	
	public PreparedStatement getPreparedStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}
	
	public Statement getStatement() throws SQLException {
		return this.connection.createStatement();
	}
	public Statement getStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return this.connection.createStatement(resultSetType, resultSetConcurrency);
	}
	public Statement getStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return this.connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/**
	 * 获取一个键
	 * @param type 键所属的类型
	 * @return 键
	 */
	public SQLKey createKey(JDBCType type) {
		return new SQLKey(this.drvier, type);
	}
	@Override
	public void close() throws SQLException {
		if(!this.connection.getAutoCommit() && !this.connection.isClosed()) {
			this.connection.commit();
			this.connection.close();
		}
	}
	
	public Connection getConnection() {
		return this.connection;
	}
	
	protected SQLPreparedStatement getSQLPreparedStatement() {
		return new SQLPreparedStatement(this);
	}
	
	protected SQLStatement getSQLStatement() {
		return new SQLStatement(this);
	}
	
	/**
	 * 获取驱动所属的数据库类型
	 */
	protected SQLDatabaseDrvier getDrvier(String name) {
		name = name.toLowerCase();
		
		if(name.startsWith("org.sqlite")) 				return SQLDatabaseDrvier.SQLite;
		if(name.startsWith("com.mysql")) 				return SQLDatabaseDrvier.MYSQL;
		if(name.startsWith("com.microsoft.sqlserver"))  return SQLDatabaseDrvier.SQLServer;
		if(name.startsWith("oracle.jdbc")) 				return SQLDatabaseDrvier.Oracle;
		if(name.startsWith("sun.jdbc.odbc")) 			return SQLDatabaseDrvier.Microsoft_Access;
		if(name.startsWith("com.ibm.db2")) 				return SQLDatabaseDrvier.IBM_DB2;
		if(name.startsWith("org.postgresql")) 			return SQLDatabaseDrvier.PostgreSQL;
		
		return SQLDatabaseDrvier.UNKNOW;
	}
}
