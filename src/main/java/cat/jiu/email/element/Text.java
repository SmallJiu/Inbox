package cat.jiu.email.element;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.email.net.msg.MsgSend;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Text {
	public static final Text empty = new Text("") {
		public void setKey(String key) {}
	};
	
	protected String key = "";
	protected Object[] args = MsgSend.SendRenderText.empty;
	
	public Text(String key, Object... args) {
		this.key = key;
		if(args!=null&&args.length>0) this.args = args; 
	}
	public Text(JsonElement json) {
		this.readFromJson(json);
	}
	public Text(NBTBase nbt) {
		this.readFromNBT(nbt);
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
	
	@SideOnly(Side.CLIENT)
	public String format() {
		return I18n.format(key, args);
	}
	
	public TextComponentTranslation toTextComponent() {
		return new TextComponentTranslation(key, args);
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
	public JsonElement writeToJson() {
		JsonElement e = null;
		
		if(this.args!=null&&this.args.length>0) {
			e = new JsonObject();
			e.getAsJsonObject().addProperty("key", this.key);
			JsonArray args = new JsonArray();
			for(int i = 0; i < this.args.length; i++) {
				args.add(String.valueOf(this.args[i]));
			}
			e.getAsJsonObject().add("args", args);
		}else if(!"".equals(this.key)) {
			e = new JsonPrimitive(this.key);
		}else {
			e = JsonNull.INSTANCE;
		}
		
		return e;
	}
	
	public void readFromJson(JsonElement json) {
		if(json instanceof JsonObject) {
			this.key = json.getAsJsonObject().get("key").getAsString();
			if(json.getAsJsonObject().has("args")) {
				JsonArray args = json.getAsJsonObject().getAsJsonArray("args");
				this.args = new Object[args.size()];
				for(int i = 0; i < this.args.length; i++) {
					this.args[i] = args.get(i).getAsString();
				}
			}
		}else if(json instanceof JsonPrimitive) {
			this.key = json.getAsString();
		}
	}
	public NBTBase writeToNBT() {
		NBTBase nbt = null;
		
		if(this.args!=null&&this.args.length>0) {
			nbt=new NBTTagCompound();
			((NBTTagCompound) nbt).setString("key", this.key);
			NBTTagList args = new NBTTagList();
			for(int i = 0; i < this.args.length; i++) {
				args.appendTag(new NBTTagString(String.valueOf(this.args[i])));
			}
			((NBTTagCompound) nbt).setTag("args", args);
		}else if(!"".equals(this.key)) {
			nbt= new NBTTagString(this.key);
		}else {
			nbt = Email.emptyTag;
		}
		return nbt;
	}
	
	public void readFromNBT(NBTBase nbt) {
		if(nbt instanceof NBTTagCompound) {
			this.key = ((NBTTagCompound)nbt).getString("key");
			if(((NBTTagCompound)nbt).hasKey("args")) {
				NBTTagList args = ((NBTTagCompound)nbt).getTagList("args", 8);
				this.args = new Object[args.tagCount()];
				for(int i = 0; i < this.args.length; i++) {
					this.args[i] = args.getStringTagAt(i);
				}
			}
		}else if(nbt instanceof NBTTagString) {
			this.key = ((NBTTagString) nbt).getString();
		}
	}
}
