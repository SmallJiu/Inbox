package cat.jiu.core.util.element;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class Text implements IText {
	public static final Object[] EMPTY_ARGS = new Object[0];
	public static final Text empty = new Text("") {
		public Text setText(String key) {return this;}
	};
	
	protected String key = "";
	protected Object[] args = EMPTY_ARGS;
	protected boolean center;
	protected boolean vanillaWrap;
	
	public Text(String key, Object... args) {
		this.setText(key);
		if(args!=null&&args.length>0) {
			this.setParameters(args);
		}
	}
	public Text(JsonObject json) {
		this.readFrom(json);
	}
	public Text(NBTTagCompound nbt) {
		this.readFrom(nbt);
	}
	
	public String getText() {
		return key;
	}
	public Text setText(String key) {
		this.key = key;
		return this;
	}
	public Object[] getParameters() {
		return args;
	}
	@Override
	public Text setParameters(Object... parameters) {
		this.args = parameters;
		return this;
	}
	
	@Override
	public boolean isCenter() {
		return this.center;
	}
	@Override
	public Text setCenter(boolean isCenter) {
		this.center = isCenter;
		return this;
	}
	
	@Override
	public boolean isVanillaWrap() {
		return this.vanillaWrap;
	}
	@Override
	public Text setUseVanillaWrap(boolean isVanillaWrap) {
		this.vanillaWrap = isVanillaWrap;
		return this;
	}
	
	public Text copy() {
		return new Text(this.writeTo(NBTTagCompound.class));
	}
	
	public JsonArray writeArgs(JsonArray args) {
		if(args==null) args = new JsonArray();
		if(this.args!=null&&this.args.length>0) {
			for(Object o : this.args) {
				args.add(String.valueOf(o));
			}
		}
		return args;
	}
	public NBTTagList writeArgs(NBTTagList args) {
		if(args==null) args = new NBTTagList();
		if(this.args!=null&&this.args.length>0) {
			for(int j = 0; j < this.args.length; j++) {
				args.appendTag(new NBTTagString(String.valueOf(this.args[j])));
			}
		}
		return args;
	}
	@Override
	public String toString() {
		return String.valueOf(this.writeTo(JsonObject.class));
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Text other = (Text) obj;
		if(!Arrays.equals(args, other.args))
			return false;
		if(key == null) {
			if(other.key != null)
				return false;
		}else if(!key.equals(other.key))
			return false;
		return true;
	}
}
