package cat.jiu.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

public class SQLPreparedStatement {
	protected final SQLDatabase db;
	protected SQLPreparedStatement(SQLDatabase db) {
		this.db = db;
	}
	
	/**
	 * 给一个表插入一个数据
	 * @param table 表名，用于指定使用哪个表
	 * @param values 数据，用于插入表的数据
	 * @return 受到影响的行数
	 * @throws SQLException
	 */
	public int insert(String table, SQLValues values) throws SQLException {
		return this.db.getPreparedStatement(new StringBuilder()
				.append("insert into ")
				.append(values.toTable(table))
				.toString()).executeUpdate();
	}

	/**
	 * 从表删除一个数据
	 * @param table 表名，用于指定使用哪个表
	 * @param where 条件，用于查询
	 * @return 受到影响的行数
	 * @throws SQLException
	 */
	public int delete(String table, SQLSelect where) throws SQLException {
		StringJoiner sql = new StringJoiner(" ", "", ";")
				.add("DELETE FROM")
				.add(table);
		if(where!=null) {
			sql.add(where.type.name())
				.add(where.toString());
		}
		return this.db.getPreparedStatement(
					sql.toString(),
					where!=null ? where.getArgs() : null)
				.executeUpdate();
	}

	/**
	 * 更行表内数据
	 * @param table 表名，用于指定使用哪个表
	 * @param values 数据，用于更新
	 * @param where 条件，用于添加条件语句，为null时会更新表内的所有数据
	 * @return 受到影响的行数
	 * @throws SQLException
	 */
	public int update(String table, SQLValues values, SQLSelect where) throws SQLException {
		StringJoiner sql = new StringJoiner(" ", "", ";")
				.add("UPDATE")
				.add(table)
				.add("SET")
				.add(values.toString());
		if(where!=null) {
			sql.add(where.type.name())
				.add(where.toString());
		}
		return this.db.getPreparedStatement(
				sql.toString(),
				where!=null ? where.getArgs() : null)
			.executeUpdate();
	}

	protected static final String[] keys = new String[] {"*"};
	/**
	 * 查找表内的数据，会查找表内的所有键的数据
	 * @param table 表名，用于指定使用哪个表
	 * @param where 条件，用于添加条件语句，为null时会查找表内的所有数据
	 * @return 查找完成后的数据
	 * @throws SQLException
	 */
	public ResultSet select(String table, SQLSelect where) throws SQLException {
		return this.select(table, keys, where);
	}

	/**
	 * 查找表内的数据
	 * @param table 表名，用于指定使用哪个表
	 * @param keys 键，用于指定查找哪个键的数据，为null时会查找表内的所有数据
	 * @param where 条件，用于添加条件语句，为null时会查找表内的所有数据
	 * @return 查找完成后的数据
	 * @throws SQLException
	 */
	public ResultSet select(String table, String[] keys, SQLSelect where) throws SQLException {
		StringJoiner key = new StringJoiner(",");
		for(int i = 0; i < keys.length; i++) {
			key.add(keys[i]);
		}
		
		StringJoiner sql = new StringJoiner(" ", "", ";")
				.add("SELECT")
				.add(key.toString())
				.add("FROM")
				.add(table);
		
		if(where!=null) {
			sql.add(where.type.name())
				.add(where.toString());
		}
		
		return this.db.getPreparedStatement(
					sql.toString(),
					where!=null ? where.getArgs() : null)
				.executeQuery();
	}

	/**
	 * 使用表连接查找表内的数据，并会查找表内的所有数据
	 * @param table1 表1的名字，用于指定使用哪个表
	 * @param table2 表2的名字，用于指定使用哪个表来连接表1
	 * @param joinType 连接格式，指定用于表连接的格式
	 * @param where 条件，用于添加条件语句，为null时会查找表内的所有数据
	 * @return 查找完成后的数据
	 * @throws SQLException
	 */
	public ResultSet joinSelect(String table1, String table2, SQLJoinType joinType, SQLSelect where) throws SQLException {
		return this.joinSelect(table1, table2, keys, joinType, where);
	}
	/**
	 * 使用表连接查找表内的数据
	 * @param table1 表1的名字，用于指定使用哪个表
	 * @param table2 表2的名字，用于指定使用哪个表来连接表1
	 * @param keys 键，用于指定查找哪个键的数据，为null时会查找表内的所有数据
	 * @param joinType 连接格式，指定用于表连接的格式
	 * @param where 条件，用于添加条件语句，为null时会查找表内的所有数据
	 * @return 查找完成后的数据
	 * @throws SQLException
	 */
	public ResultSet joinSelect(String table1, String table2, String[] keys, SQLJoinType joinType, SQLSelect where) throws SQLException {
		StringJoiner key = new StringJoiner(",");
		for(int i = 0; i < keys.length; i++) {
			key.add(keys[i]);
		}
		
		StringJoiner sql = new StringJoiner(" ", "", ";")
				.add("SELECT")
				.add(key.toString())
				.add("FROM")
				.add(table1)
				.add(joinType.sql)
				.add(table2);
		
		if(where!=null) {
				sql.add(where.type.name())
				.add(where.toString());
		}
		return this.db.getPreparedStatement(
					sql.toString(),
					where!=null ? where.getArgs() : null)
				.executeQuery();
	}
	
	/**
	 * 创建一个表
	 * @param name 需要创建的表名
	 * @param table 表内的键
	 * @return
	 * @throws SQLException
	 */
	public boolean createTable(String name, SQLTableKey table) throws SQLException {
		StringJoiner sql = new StringJoiner(" ", "", ";")
				.add("CREATE TABLE if not exists")
				.add(name)
				.add(table.toString());
		return this.execute(sql.toString());
	}
	
	public boolean dropIndex(String table, String index) throws SQLException {
		switch(this.db.drvier) {
			case MYSQL: return this.execute("ALTER TABLE " + table + " DROP INDEX " + index);
			case SQLServer: return this.execute("DROP INDEX " + table + "." + index); 
			default:
				break;
		}
		return false;
	}

	/**
	 * 删除指定表
	 * @param name 表名，用于指定删除哪个表
	 * @return
	 * @throws SQLException
	 */
	public boolean dropTable(String name) throws SQLException {
		return this.execute("DROP TABLE if exists " + name);
	}
	/**
	 * 删除指定表内的所有数据
	 * @param name 表名，用于指定删除哪个表的数据
	 * @return
	 * @throws SQLException
	 */
	public boolean dropTableAllValues(String name) throws SQLException {
		return this.execute("DELETE FROM " + name);
	}
	/**
	 * 删除数据库
	 * @param name 数据库名，用于指定删除哪个数据库
	 * @return
	 * @throws SQLException
	 */
	public boolean dropDatabase(String name) throws SQLException {
		return this.execute("DROP DATABASE if exists " + name);
	}

	/**
	 * 使用PreparedStatement执行sql语句，但不替换语句内的通配符
	 * @param sql 需要执行的sql语句
	 * @return
	 * @throws SQLException
	 */
	public boolean execute(String sql) throws SQLException {
		return this.db.connection.prepareStatement(sql).execute();
	}
	
	/**
	 * 使用PreparedStatement执行sql语句
	 * @param sql 需要执行的sql语句
	 * @param whereArgs sql语句内通配符的替换数据
	 * @return
	 * @throws SQLException
	 */
	public boolean execute(String sql, Object[] whereArgs) throws SQLException {
		return this.db.getPreparedStatement(sql, whereArgs).execute();
	}
}
