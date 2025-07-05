package cat.jiu.email.util;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.Inbox;
import cat.jiu.sql.*;
import cat.jiu.sql.select.Where;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBParser {
    public static final String
            DB_PREFIX = "jdbc:sqlite:",
            DB_TABLE = "inboxes",
            DB_TABLE_KEY_UUID = "uuid",
            DB_TABLE_KEY_INBOX = "inbox"
    ;

    public static String getDBUrl() {
        if(true) {
            EmailConfigServer.SQL_PROPERTIES.Database_Driver.get().loadDriver();
            return EmailConfigServer.SQL_PROPERTIES.Database_Driver.get().url(EmailConfigServer.SQL_PROPERTIES.Database_Url.get().replace("{root}", EmailAPI.getSaveEmailRootPath()));
        }
        return DBParser.DB_PREFIX + EmailAPI.getSaveEmailRootPath() + File.separator + "inbox.db";
    }

    public static SQLDatabase getDatabase(String url) throws SQLException {
        SQLDatabase db = new SQLDatabase(url);
        db.prepared.createTable(DBParser.DB_TABLE, new SQLTableKey()
                .put(DBParser.DB_TABLE_KEY_UUID, db.createKey(JDBCType.VARCHAR)
                        .setNotNull(true)
                        .setPrimaryKey(true))
                .put(DBParser.DB_TABLE_KEY_INBOX, db.createKey(JDBCType.VARCHAR)
                        .setNotNull(true))
        );
        return db;
    }

    public static boolean write(String dbURL, Inbox inbox) throws Exception {
        try(SQLDatabase db = getDatabase(dbURL)) {
            db.prepared.delete(DBParser.DB_TABLE, new SQLSelect(SQLSelectType.WHERE, "'" + inbox.getOwner() + "'")
                    .add(new Where(DBParser.DB_TABLE_KEY_UUID, SQLOperator.EQUAL, "?", null)));

            db.prepared.insert(DBParser.DB_TABLE, inbox.write(new SQLValues()));
            return true;
        }
    }

    public static JsonObject read(String dbURL, String uid) throws Exception {
        try(SQLDatabase db = getDatabase(dbURL)) {
            try(ResultSet rs = db.prepared.select(DBParser.DB_TABLE, new SQLSelect(SQLSelectType.WHERE, uid)
                    .add(new Where(DBParser.DB_TABLE_KEY_UUID, SQLOperator.EQUAL, "?", null)))) {
                if(rs.next()) {
                    String str = rs.getString(DBParser.DB_TABLE_KEY_INBOX);
                    if(!str.isEmpty()) {
                        return JsonParser.parseString(str).getAsJsonObject();
                    }
                }
            }
        }
        return new JsonObject();
    }
}
