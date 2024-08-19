package cat.jiu.email.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings("unchecked")
public final class JsonParser {
	public static final Gson gson = new GsonBuilder().serializeNulls().create();
	public static final Gson gson_format = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	public static final com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
	public static <T extends JsonElement> T parse(File file) {
		try {
			return (T) parser.parse(new InputStreamReader(new FileInputStream(file)));
		}catch(JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static <T extends JsonElement> T parse(String path) {
		try {
			return (T) parser.parse(new InputStreamReader(new FileInputStream(path)));
		}catch(JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <T extends JsonElement> T parse(InputStream path) {
		try {
			return (T) parser.parse(new InputStreamReader(path));
		}catch(JsonIOException | JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean toJsonFile(String path, Object src, boolean format) {
		return toJsonFile(new File(path), src, format);
	}
	public static boolean toJsonFile(File file, Object src, boolean format) {
		try {
			if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
	        if (file.exists()) file.delete();
	        
	        file.createNewFile();
	        OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file));
            write.write((format ? gson_format : gson).toJson(src));
            write.flush();
            write.close();
	        return true;
		} catch (Exception e) {e.printStackTrace();return false;}
	}
}
