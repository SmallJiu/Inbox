package cat.jiu.email.util;

import com.google.gson.*;

import java.io.*;
import java.nio.file.Files;

@SuppressWarnings("unchecked")
public final class JsonUtil {
	public static final Gson gson = new GsonBuilder().serializeNulls().create();
	public static final JsonParser parser = new JsonParser();
	public static <T extends JsonElement> T parse(File file) {
		if(!file.exists()) return null;
		try {
			return (T) parser.parse(new InputStreamReader(new FileInputStream(file)));
		}catch(JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static <T extends JsonElement> T parse(String path) {
		try {
			File file = new File(path);
			if(!file.exists()) return null;
			return (T) parser.parse(new InputStreamReader(new FileInputStream(file)));
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
		String json = src instanceof JsonElement ? String.valueOf(src) : gson.toJson(src);
		
		try {
			File file = new File(path);
			if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
	        if (file.exists()) file.delete();
	        
	        file.createNewFile();
	        OutputStreamWriter write = new OutputStreamWriter(Files.newOutputStream(file.toPath()));
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
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < number; i++) {
            result.append("	");
        }
        return result.toString();
    }
}
