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
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings("unchecked")
public final class JsonUtil {
	public static final Gson gson = new GsonBuilder().serializeNulls().create();
	public static final JsonParser parser = new JsonParser();
	public static <E extends JsonElement> E parse(File file) {
		if(!file.exists()) return null;
		try {
			return (E) parser.parse(new InputStreamReader(new FileInputStream(file)));
		}catch(JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static <E extends JsonElement> E parse(String path) {
		try {
			File file = new File(path);
			if(!file.exists()) return null;
			return (E) parser.parse(new InputStreamReader(new FileInputStream(file)));
		}catch(JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <E extends JsonElement> E parse(InputStream path) {
		try {
			return (E) parser.parse(new InputStreamReader(path));
		}catch(JsonIOException | JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean toJsonFile(String path, Object src, boolean format) {
		String json = src instanceof JsonElement ? String.valueOf(src) : gson.toJson(src);
		
		try {
			File file = new File(path);
			if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
	        if (file.exists()) file.delete();
	        
	        file.createNewFile();
	        OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file));
            write.write(format ? formatJson(json) : json);
            write.flush();
            write.close();
	        return true;
		} catch (Exception e) {e.printStackTrace();return false;}
	}
	
	public static String formatJson(String json) {
        StringBuffer result = new StringBuffer();
        int number = 0;
        
        for (int i = 0; i < json.length(); i++) {
        	char key = json.charAt(i);
            if (key == '[' || key == '{') {
        		result.append(key);
        		if(i-1 > 0) {
            		if(json.charAt(i-1) != '\"') {
                        result.append('\n');
                        number++;
                        result.append(indent(number));
            		}
            	}else {
                    result.append('\n');
                    number++;
                    result.append(indent(number));
            	}
                continue;
            }
            
            if ((key == ']' || key == '}')) {
            	if(i+1 < json.length()) {
            		if(json.charAt(i+1) != '\"') {
                		result.append('\n');
                        number--;
                        result.append(indent(number));
                	}
            	}else {
            		result.append('\n');
                    number--;
                    result.append(indent(number));
            	}
                result.append(key);
                continue;
            }
            
            if (key == ',') {
            	result.append(key);
            	if(canNextLine(json.charAt(i-1))) {
                    result.append('\n');
                    result.append(indent(number));
            	}else {
            		if(json.substring(i-4, i).equals("true") 
            		|| json.substring(i-5, i).equals("false")
            		|| json.substring(i-4, i).equals("null")) {
                        result.append('\n');
                        result.append(indent(number));
            		}
            	}
                continue;
            }
            
            if(key == ':') {
        		result.append(key);
            	if(json.charAt(i-1) == '"') {
	            	result.append(' ');
            	}
            	continue;
            }
            result.append(key);
        }
        
        result.append('\n');
        return result.toString();
    }
	
	private static boolean canNextLine(char c) {
		switch(c) {
			case ']':
			case '"':
			case '}':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9': return true;
			default: return false;
		}
	}
	
	private static String indent(int number) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < number; i++) {
            result.append("	");
        }
        return result.toString();
    }
}
