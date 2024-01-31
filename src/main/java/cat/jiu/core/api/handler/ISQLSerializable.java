package cat.jiu.core.api.handler;

import java.sql.ResultSet;
import java.sql.SQLException;

import cat.jiu.sql.SQLValues;

public interface ISQLSerializable {
	default SQLValues write(SQLValues value) {return value;}
	default void read(ResultSet result) throws SQLException{}
}
