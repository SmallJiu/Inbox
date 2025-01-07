package cat.jiu.email.util;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Inbox;
import cat.jiu.sql.*;
import cat.jiu.sql.select.Where;
import com.google.gson.JsonObject;

import java.io.File;
import java.sql.JDBCType;
import java.sql.ResultSet;

public class DBParser {
    public static final String
            DB_PREFIX = "jdbc:sqlite:",
            DB_URL = DBParser.DB_PREFIX + EmailAPI.getSaveEmailRootPath() + File.separator + "inbox.db",
            DB_TABLE = "inboxes",
            DB_TABLE_KEY_UUID = "uuid",
            DB_TABLE_KEY_INBOX = "inbox"
     ;

    public static boolean saveInboxToDB(String dbURL, Inbox inbox) {
        try(SQLDatabase db = new SQLDatabase(dbURL)) {
            db.prepared.createTable(DBParser.DB_TABLE, new SQLTableKey()
                    .put(DBParser.DB_TABLE_KEY_UUID, db.createKey(JDBCType.VARCHAR)
                            .setNotNull(true)
                            .setPrimaryKey(true))
                    .put(DBParser.DB_TABLE_KEY_INBOX, db.createKey(JDBCType.VARCHAR)
                            .setNotNull(true))
            );

            db.prepared.delete(DBParser.DB_TABLE, new SQLSelect(SQLSelectType.WHERE, "'" + inbox.getOwner() + "'")
                    .add(new Where(DBParser.DB_TABLE_KEY_UUID, SQLOperator.EQUAL, "?", null)));

            db.prepared.insert(DBParser.DB_TABLE, inbox.writeTo(SQLValues.class));
            return true;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static JsonObject getInboxJson(String dbURL, String uid) {
        try(SQLDatabase db = new SQLDatabase(dbURL)) {
            db.prepared.createTable(DBParser.DB_TABLE, new SQLTableKey()
                    .put(DBParser.DB_TABLE_KEY_UUID, db.createKey(JDBCType.VARCHAR)
                            .setNotNull(true)
                            .setPrimaryKey(true))
                    .put(DBParser.DB_TABLE_KEY_INBOX, db.createKey(JDBCType.VARCHAR)
                            .setNotNull(true))
            );
            ResultSet rs = db.prepared.select(DBParser.DB_TABLE, new SQLSelect(SQLSelectType.WHERE, uid)
                    .add(new Where(DBParser.DB_TABLE_KEY_UUID, SQLOperator.EQUAL, "?", null)));
            if(rs.next()) {
                String str = rs.getString(DBParser.DB_TABLE_KEY_INBOX);
                if(!str.isEmpty()) {
                    return JsonParser.parser.parse(str).getAsJsonObject();
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return new JsonObject();
    }
}
