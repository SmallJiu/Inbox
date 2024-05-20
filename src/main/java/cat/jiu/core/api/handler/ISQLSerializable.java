package cat.jiu.core.api.handler;

import java.sql.ResultSet;
import java.sql.SQLException;

import cat.jiu.sql.SQLValues;

public interface ISQLSerializable {
	SQLValues write(SQLValues value);
	void read(ResultSet result) throws SQLException;
}
