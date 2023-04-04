package cat.jiu.email.element;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.net.msg.SendRenderText;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class InboxText implements IInboxText {
	public static final InboxText empty = new InboxText("") {
		public void setText(String key) {}
	};
	
	protected String key = "";
	protected Object[] args = SendRenderText.empty;
	
	public InboxText(String key, Object... args) {
		this.setText(key);
		if(args!=null&&args.length>0) {
			this.setParameters(args);
		}
	}
	public InboxText(JsonElement json) {
		this.readFromJson(json);
	}
	public InboxText(NBTBase nbt) {
		this.readFromNBT(nbt);
	}
	
	public String getText() {
		return key;
	}
	public void setText(String key) {
		this.key = key;
	}
	public Object[] getParameters() {
		return args;
	}
	@Override
	public void setParameters(Object... parameters) {
		this.args = parameters;
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
		return String.valueOf(this.writeToJson());
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
		InboxText other = (InboxText) obj;
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
