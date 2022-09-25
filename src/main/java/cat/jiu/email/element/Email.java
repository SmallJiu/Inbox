package cat.jiu.email.element;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.util.JsonToStackUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class Email implements INBTSerializable, IJsonSerializable {
	protected Message title;
	protected String time;
	protected String sender;
	protected EmailSound sound;
	protected List<ItemStack> items;
	protected List<Message> msgs;
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
	public Email(Message title, String sender, EmailSound sound, List<ItemStack> items, List<Message> msgs) {
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
	public Message getTitle() {return title;}
	public String getSender() {return sender;}
	public EmailSound getSound() {return sound;}
	public boolean isRead() {return read;}
	public boolean isReceived() {return accept;}
	public List<ItemStack> getItems() {return Lists.newArrayList(items);}
	public List<Message> getMsgs() {return msgs;}

	public void setSender(String sender) {this.sender = sender;}
	public void setTitle(Message title) {this.title = title;}
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
			this.msgs.add(new Message(msg, args));
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
		
		json.add("title", this.title.write(new JsonObject()));
		json.addProperty("time", this.time);
		json.addProperty("sender", this.sender);
		if(read) json.addProperty("read", true);
		if(accept) json.addProperty("accept", true);
		if(this.hasSound()) json.add("sound", this.sound.toJson());
		if(this.hasItems()) {
			json.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		}
		if(this.hasMessages()) {
			JsonObject msgs = new JsonObject();
			for(int i = 0; i < this.msgs.size(); i++) {
				Message msg = this.msgs.get(i);
				msgs.add(msg.key, msg.writeArgs(new JsonArray()));
			}
			json.add("msgs", msgs);
		}
		return json;
	}

	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			this.title = new Message(json.get("title").getAsJsonObject());
			this.time = json.get("time").getAsString();
			this.sender = json.get("sender").getAsString();
			if(json.has("read")) this.read = true;
			if(json.has("accept")) this.accept = true;
			if(json.has("sound")) this.sound = EmailSound.from(json.get("sound").getAsJsonObject());
			if(json.has("items")) {
				this.items = JsonToStackUtil.toStacks(json.get("items"));
			}
			if(json.has("msgs")) {
				this.msgs = Lists.newArrayList();
				
				JsonObject msgs = json.get("msgs").getAsJsonObject();
				for(Entry<String, JsonElement> msg : msgs.entrySet()) {
					String key = msg.getKey();
					JsonArray value = msg.getValue().getAsJsonArray();
					if(value.size()>0) {
						Object[] arg = new Object[value.size()];
						for(int i = 0; i < arg.length; i++) {
							arg[i] = value.get(i).getAsString();
						}
						this.msgs.add(new Message(key, arg));
					}else {
						if(key.isEmpty()) {
							this.msgs.add(Message.empty);
						}else {
							this.msgs.add(new Message(key));
						}
					}
				}
			}
		}
	}

	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		
		nbt.setTag("title", this.title.write(new NBTTagCompound()));
		nbt.setString("time", this.time);
		nbt.setString("sender", this.sender);
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
				Message msg = this.msgs.get(i);
				msgs.setTag(msg.key, msg.writeArgs(new NBTTagList()));
			}
			nbt.setTag("msgs", msgs);
		}
		
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			this.title = new Message(nbt.getCompoundTag("title"));
			this.time = nbt.getString("time");
			this.sender = nbt.getString("sender");
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
				for(String key : msgs.getKeySet()) {
					NBTTagList argList = msgs.getTagList(key, 8);
					if(argList.tagCount()>0) {
						Object[] arg = new Object[argList.tagCount()];
						for(int i = 0; i < arg.length; i++) {
							arg[i] = argList.getStringTagAt(i);
						}
						this.msgs.add(new Message(key, arg));
					}else {
						if(key.isEmpty()) {
							this.msgs.add(Message.empty);
						}else {
							this.msgs.add(new Message(key));
						}
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
