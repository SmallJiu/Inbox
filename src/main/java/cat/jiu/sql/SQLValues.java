package cat.jiu.sql;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

public class SQLValues {
	private final TreeMap<String, Object> values = new TreeMap<>();
	public SQLValues() {}
	public SQLValues(Map<String, Object> values) {
		this.values.putAll(values);
	}
	public SQLValues(SQLValues values) {
		this.values.putAll(values.values);
	}
	
	public int size() {
		return this.values.size();
	}
    public boolean isEmpty() {
    	return this.values.isEmpty();
    }
	public Object get(String key) {
		return this.values.get(key);
	}
	public void remove(String key) {
		this.values.remove(key);
	}
	public void clear() {
		this.values.clear();
	}
	public boolean containsKey(String key) {
		return this.values.containsKey(key);
	}
	public Set<Entry<String, Object>> valueSet() {
		return this.values.entrySet();
	}
	public Set<String> keySet() {
		return this.values.keySet();
	}
	
	public SQLValues putNull(String key) {
		this.values.put(key, null); 
		return this;
	}
	public SQLValues put(String key, Object value) {
        this.values.put(key, value);
		return this;
	}
	
	public String getAsString(String key, String failBack) {
		Object o = this.values.get(key);
		if(o instanceof String) {
			return (String)o;
		}
		return o!=null ? o.toString() : failBack;
	}
	
	public Number getAsNumber(String key, Number failBack) {
		Object o = this.values.get(key);
		if(o instanceof Number) {
			return (Number)o;
		}
		return failBack;
	}
	
	public Byte getAsByte(String key, Byte failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Byte) {
				return (Byte) num;
			}
			return num.byteValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Byte.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Short getAsShort(String key, Short failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Short) {
				return (Short) num;
			}
			return num.shortValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Short.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Integer getAsInt(String key, Integer failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Integer) {
				return (Integer) num;
			}
			return num.intValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Integer.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Long getAsLong(String key, Long failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Long) {
				return (Long) num;
			}
			return num.longValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Long.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Float getAsFloat(String key, Float failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Float) {
				return (Float) num;
			}
			return num.floatValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Float.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Double getAsDouble(String key, Double failBack) {
		Number num = this.getAsNumber(key, failBack);
		if(num!=null) {
			if(num instanceof Double) {
				return (Double) num;
			}
			return num.doubleValue();
		}else {
			Object o = this.values.get(key);
			if(o instanceof CharSequence) {
				try {
					return Double.valueOf(o.toString());
				}catch(Exception e) {e.printStackTrace();}
			}
		}
		return failBack;
	}
	
	public Boolean getAsBoolean(String key, Boolean failBack) {
		Object o = this.values.get(key);
		if(o instanceof Boolean) {
			return (Boolean) o;
		}else {
			if(o instanceof CharSequence) {
				try {
					return Boolean.valueOf(o.toString()) || "1".equals(o);
				}catch(Exception e) {e.printStackTrace();}
			}else if(o instanceof Number) {
				return this.getAsInt(key, failBack ? 1 : 0) != 0;
			}
		}
		return failBack;
	}
	
	public byte[] getAsByteArray(String key, byte[] failBack) {
		Object o = this.values.get(key);
		if(o instanceof byte[]) {
			return (byte[]) o;
		}
		return failBack;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public ArrayList<String> getAsStringList(String key){
		Object o = this.values.get(key);
		if(o instanceof ArrayList) {
			ArrayList list = (ArrayList) o;
			for(Object obj : list) {
				if(obj instanceof String) return list;
				break;
			}
		}
		return null;
	}
	
	public String toTable(String table) {
		StringJoiner sb = new StringJoiner(" ", "", ";");
		sb.add(table);
		
		StringJoiner keys = new StringJoiner(",", "(", ")");
		StringJoiner values = new StringJoiner(",", "(", ")");
		for(Entry<String, Object> value : this.valueSet()) {
			keys.add(value.getKey());
			values.add(String.valueOf(value.getValue()));
		}
		sb.add(keys.toString())
			.add("values")
			.add(values.toString());
		
		return sb.toString();
	}
	
	@Override
    public String toString() {
		StringJoiner s = new StringJoiner(",");
        for (String name : this.values.keySet()) {
            s.add(name + "=" + String.valueOf(this.get(name)));
        }
        return s.toString();
	}
	
	@Override
	public int hashCode() {
		return this.values.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		SQLValues other = (SQLValues) obj;
		if(this.values == null) {
			if(other.values != null) return false;
		}else if(!this.values.equals(other.values)) return false;
		return true;
	}
}
