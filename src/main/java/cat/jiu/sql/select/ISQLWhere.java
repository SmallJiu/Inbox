package cat.jiu.sql.select;

import cat.jiu.sql.SQLLogicOperator;
import cat.jiu.sql.SQLOperator;

public interface ISQLWhere {
	String getId();
	SQLOperator getOperator();
	SQLLogicOperator getNextOperator();
	String toSQL();
}
