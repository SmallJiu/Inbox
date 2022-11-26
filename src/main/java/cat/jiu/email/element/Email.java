package cat.jiu.email.element;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.util.JsonToStackUtil;
import cat.jiu.email.util.NBTTagNull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class Email implements ISerializable {
	protected Text title;
	protected String time;
	protected Text sender;
	protected EmailSound sound;
	protected List<ItemStack> items;
	protected List<Text> msgs;
	protected boolean read;
	protected boolean accept;
	
	/**
	 * @param title the email title
	 * @param sender the send email sender
	 * @param sound the email sound
	 * @param items the email items
	 * @param msgs the email messages
	 * 
	 * @author small_jiu
	 */
	public Email(Text title, Text sender, EmailSound sound, List<ItemStack> items, List<Text> msgs) {
		this.title = title;
		this.time = MsgSend.getTime();
		this.sender = sender;
		this.sound = sound;
		this.items = items;
		this.msgs = msgs;
	}
	
	public Email(NBTTagCompound nbt) {
		this.read(nbt);
	}
	public Email(JsonObject json) {
		this.read(json);
	}
	
	public String getTime() {return time;}
	public Text getTitle() {return title;}
	public Text getSender() {return sender;}
	public EmailSound getSound() {return sound;}
	public boolean isRead() {return read;}
	public boolean isReceived() {return accept;}
	public List<ItemStack> getItems() {return Lists.newArrayList(items);}
	public List<Text> getMsgs() {return msgs;}

	public void setSender(Text sender) {this.sender = sender;}
	public void setTitle(Text title) {this.title = title;}
	public void setRead(boolean read) {this.read = read;}
	public void setAccept(boolean accept) {this.accept = accept;}
	public void setTime(LocalDateTime time) {this.time = MsgSend.dateFormat.format(time);}
	public void setTime(Date time) {this.time = MsgSend.dateFormat.format(time);}
	public void clearItems() {
		if(this.items!=null) {
			this.items.clear();
		}
	}
	public void addItem(ItemStack stack) {
		if(this.items==null) this.items = Lists.newArrayList();
		if(stack!=null && !stack.isEmpty() && this.items.size()<16) {
			this.items.add(stack);
		}
	}
	public void addMessage(String msg, Object... args) {
		if(msg!=null && !msg.isEmpty()) {
			this.msgs.add(new Text(msg, args));
		}
	}
	public boolean hasSound() {
		return this.sound!=null;
	}
	public boolean hasItems() {
		return this.items!=null && !this.items.isEmpty();
	}
	public boolean hasMessages() {
		return this.msgs!=null && !this.msgs.isEmpty();
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		
		json.add("title", this.title.writeToJson());
		json.addProperty("time", this.time);
		json.add("sender", this.sender.writeToJson());
		if(read) json.addProperty("read", true);
		if(accept) json.addProperty("accept", true);
		if(this.hasSound()) json.add("sound", this.sound.toJson());
		if(this.hasItems()) {
			json.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		}
		if(this.hasMessages()) {
			JsonObject msgs = new JsonObject();
			for(int i = 0; i < this.msgs.size(); i++) {
				Text msg = this.msgs.get(i);
				if(msg.args!=null && msg.args.length>0) {
					JsonArray arg = new JsonArray();
					for(int j = 0; j < msg.args.length; j++) {
						arg.add(String.valueOf(msg.args[j]));
					}
					msgs.add(msg.key, arg);
				}else {
					msgs.add(msg.key, JsonNull.INSTANCE);
				}
			}
			json.add("msgs", msgs);
		}
		return json;
	}
	
	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			this.title = new Text(json.get("title"));
			this.time = json.get("time").getAsString();
			this.sender = new Text(json.get("sender"));
			if(json.has("read")) this.read = true;
			if(json.has("accept")) this.accept = true;
			if(json.has("sound")) this.sound = EmailSound.from(json.get("sound").getAsJsonObject());
			if(json.has("items")) {
				this.items = JsonToStackUtil.toStacks(json.get("items"));
			}
			if(json.has("msgs")) {
				this.msgs = Lists.newArrayList();
				JsonObject msgs = json.getAsJsonObject("msgs");
				
				for(Entry<String, JsonElement> msg : msgs.entrySet()) {
					String key = msg.getKey();
					JsonElement a = msg.getValue();
					if(a.isJsonArray()) {
						JsonArray argJson = a.getAsJsonArray();
						Object[] args = new Object[argJson.size()];
						for(int i = 0; i < args.length; i++) {
							args[i] = argJson.get(i).getAsString();
						}
						this.msgs.add(new Text(key, args));
					}else {
						this.msgs.add(new Text(key));
					}
				}
			}
		}
	}

	static final boolean debug = true;
	static final NBTBase emptyTag = debug ? new NBTTagNull() : new NBTTagByte((byte)0);

	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		
		nbt.setTag("title", this.title.writeToNBT());
		nbt.setString("time", this.time);
		nbt.setTag("sender", this.sender.writeToNBT());
		if(read) nbt.setBoolean("read", true);
		if(accept) nbt.setBoolean("accept", true);
		if(this.hasSound()) nbt.setTag("sound", this.sound.toNBT());
		if(this.hasItems()) {
			NBTTagCompound items = new NBTTagCompound();
			for(int i = 0; i < this.items.size(); i++) {
				items.setTag(Integer.toString(i), this.items.get(i).writeToNBT(new NBTTagCompound()));
			}
			nbt.setTag("items", items);
		}
		if(this.hasMessages()) {
			NBTTagCompound msgs = new NBTTagCompound();
			for(int i = 0; i < this.msgs.size(); i++) {
				Text msg = this.msgs.get(i);
				if(msg.args!=null && msg.args.length>0) {
					NBTTagList args = new NBTTagList();
					for(int j = 0; j < msg.args.length; j++) {
						args.appendTag(new NBTTagString(String.valueOf(msg.args[i])));
					}
					msgs.setTag(msg.key, args);
				}else {
					msgs.setTag(msg.key, emptyTag);
				}
			}
			nbt.setTag("msgs", msgs);
		}
		
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			this.title = new Text(nbt.getTag("title"));
			this.time = nbt.getString("time");
			this.sender = new Text(nbt.getTag("sender"));
			if(nbt.hasKey("read")) this.read = true;
			if(nbt.hasKey("accept")) this.accept = true;
			if(nbt.hasKey("sound")) this.sound = EmailSound.from(nbt.getCompoundTag("sound"));
			if(nbt.hasKey("items")) {
				this.items = Lists.newArrayList();
				NBTTagCompound items = nbt.getCompoundTag("items");
				for(String item : items.getKeySet()) {
					this.items.add(new ItemStack(items.getCompoundTag(item)));
				}
			}
			if(nbt.hasKey("msgs")) {
				this.msgs = Lists.newArrayList();
				NBTTagCompound msgs = nbt.getCompoundTag("msgs");
				List<String> keys = Lists.newArrayList(msgs.getKeySet());
				for(int i = 0; i < keys.size(); i++) {
					String key = keys.get(i);
					NBTBase msg = msgs.getTag(key);
					if(msg instanceof NBTTagNull || msg instanceof NBTTagByte) {
						this.msgs.add(new Text(key));
					}else if(msg instanceof NBTTagList) {
						NBTTagList argNBT = (NBTTagList) msg;
						Object[] args = new Object[argNBT.tagCount()];
						for(int j = 0; j < args.length; j++) {
							args[j] = argNBT.getStringTagAt(j);
						}
						this.msgs.add(new Text(key, args));
					}
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return this.write(new JsonObject()).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (accept ? 1231 : 1237);
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		result = prime * result + ((msgs == null) ? 0 : msgs.hashCode());
		result = prime * result + (read ? 1231 : 1237);
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + ((sound == null) ? 0 : sound.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
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
		Email other = (Email) obj;
		if(accept != other.accept)
			return false;
		if(items == null) {
			if(other.items != null)
				return false;
		}else if(!items.equals(other.items))
			return false;
		if(msgs == null) {
			if(other.msgs != null)
				return false;
		}else if(!msgs.equals(other.msgs))
			return false;
		if(read != other.read)
			return false;
		if(sender == null) {
			if(other.sender != null)
				return false;
		}else if(!sender.equals(other.sender))
			return false;
		if(sound == null) {
			if(other.sound != null)
				return false;
		}else if(!sound.equals(other.sound))
			return false;
		if(time == null) {
			if(other.time != null)
				return false;
		}else if(!time.equals(other.time))
			return false;
		if(title == null) {
			if(other.title != null)
				return false;
		}else if(!title.equals(other.title))
			return false;
		return true;
	}
}