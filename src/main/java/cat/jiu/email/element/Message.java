package cat.jiu.email.element;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.email.net.msg.MsgSend;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class Message implements INBTSerializable, IJsonSerializable {
	public static final Message empty = new Message("") {
		public void setKey(String key) {}
	};
	String key;
	Object[] args = MsgSend.SendRenderText.empty;
	
	public Message(String key, Object... args) {
		this.key = key;
		this.args = args;
	}
	public Message(JsonObject json) {
		this.read(json);
	}
	public Message(NBTTagCompound nbt) {
		this.read(nbt);
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Object[] getArgs() {
		return args;
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
		Message other = (Message) obj;
		if(!Arrays.equals(args, other.args))
			return false;
		if(key == null) {
			if(other.key != null)
				return false;
		}else if(!key.equals(other.key))
			return false;
		return true;
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json=new JsonObject();
		
		json.addProperty("key", this.key);
		if(this.args!=null&&this.args.length>0) {
			JsonArray args = new JsonArray();
			for(int i = 0; i < this.args.length; i++) {
				args.add(String.valueOf(this.args[i]));
			}
			json.add("args", args);
		}
		
		return json;
	}
	@Override
	public void read(JsonObject json) {
		if(json==null||json.size()<1) {
			this.key = "";
		}else {
			this.key = json.get("key").getAsString();
			if(json.has("args")) {
				JsonArray args = json.getAsJsonArray("args");
				this.args = new Object[args.size()];
				for(int i = 0; i < this.args.length; i++) {
					this.args[i] = args.get(i).getAsString();
				}
			}
		}
	}
	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt=new NBTTagCompound();
		
		nbt.setString("key", this.key);
		if(this.args!=null&&this.args.length>0) {
			NBTTagList args = new NBTTagList();
			for(int i = 0; i < this.args.length; i++) {
				args.appendTag(new NBTTagString(String.valueOf(this.args[i])));
			}
			nbt.setTag("args", args);
		}
		
		return nbt;
	}
	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt==null||nbt.getSize()<1) {
			this.key="";
		}else {
			this.key = nbt.getString("key");
			if(nbt.hasKey("args")) {
				NBTTagList args = nbt.getTagList("args", 8);
				this.args = new Object[args.tagCount()];
				for(int i = 0; i < this.args.length; i++) {
					this.args[i] = args.getStringTagAt(i);
				}
			}
		}
	}
}
