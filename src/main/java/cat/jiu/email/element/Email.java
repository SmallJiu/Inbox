package cat.jiu.email.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.email.iface.IInboxSound;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;
import cat.jiu.email.util.TimeMillis;
import cat.jiu.sql.SQLValues;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class Email implements ISerializable {
	protected IInboxText title;
	
	protected long create_time;
	protected String create_time_s;
	
	protected TimeMillis expiration_time;
	protected long expiration_time_l;
	
	protected IInboxText sender;
	protected IInboxSound sound;
	protected List<ItemStack> items;
	protected List<IInboxText> msgs;
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
	public Email(@Nonnull IInboxText title, @Nonnull IInboxText sender, IInboxSound sound, List<ItemStack> items, List<IInboxText> msgs) {
		this.title = title;
		this.create_time = System.currentTimeMillis();
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
	
	@Deprecated
	public String getTime() {
		return this.getCreateTimeAsString();
	}
	public long getCreateTimeAsTimestamp() {
		return create_time;
	}
	public String getCreateTimeAsString() {
		if(this.create_time_s == null) {
			this.create_time_s = EmailUtils.dateFormat.format(new Date(this.create_time));
		}
		return this.create_time_s;
	}
	public IInboxText getTitle() {return title;}
	public IInboxText getSender() {return sender;}
	public IInboxSound getSound() {return sound;}
	public boolean isRead() {return read;}
	public boolean isReceived() {return accept;}
	public List<ItemStack> getItems() {return Lists.newArrayList(items);}
	public List<IInboxText> getMsgs() {return msgs;}
	
	protected long networkSize = -404;
	public long getEmailNetworkSize() {
		if(this.networkSize == -404) {
			this.networkSize = EmailUtils.getSize(this.writeTo(NBTTagCompound.class));
		}
		return this.networkSize;
	}

	public void setSender(IInboxText sender) {this.sender = sender;}
	public void setTitle(IInboxText title) {this.title = title;}
	public void setRead(boolean read) {this.read = read;}
	public void setAccept(boolean accept) {this.accept = accept;}
	@Deprecated
	public void setTime(LocalDateTime time) {
		this.create_time = time.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		this.create_time_s = null;
	}
	@Deprecated
	public void setTime(Date time) {
		this.create_time = time.getTime();
		this.create_time_s = null;
	}
	public void setCreateTime(LocalDateTime time) {
		this.create_time = time.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		this.create_time_s = null;
	}
	public void setCreateTime(Date time) {
		this.create_time = time.getTime();
		this.create_time_s = null;
	}
	public void setCreateTimeToNow() {
		this.create_time = System.currentTimeMillis();
		this.create_time_s = null;
	}
	
	public void setExpirationTime(TimeMillis expiration_time) {
		if(this.expiration_time == null || this.expiration_time.millis == 0) {
			this.expiration_time = expiration_time;
		}
	}
	public TimeMillis getExpirationTime() {
		return expiration_time;
	}
	
	public long getExpirationTimeAsTimestamp() {
		if(this.expiration_time_l == -404 || this.expiration_time_l <= 10 && this.expiration_time != null) {
			this.expiration_time_l = this.create_time + this.expiration_time.millis;
		}
		return this.expiration_time_l;
	}
	
	public ItemStack removeItem(int slot) {
		if(this.items!=null && slot >= 0 && slot < this.items.size()) {
			return this.items.remove(slot);
		}
		return null;
	}
	public void clearItems() {
		if(this.items!=null) {
			this.items.clear();
		}
	}
	public void addItems(List<ItemStack> stacks) {
		if(this.items==null) this.items = Lists.newArrayList();
		if(stacks!=null && !stacks.isEmpty()) {
			for(int i = 0; i < stacks.size() && this.items.size()<16; i++) {
				this.items.add(stacks.get(i));
			}
		}
	}
	public void addItem(ItemStack stack) {
		if(this.items==null) this.items = Lists.newArrayList();
		if(stack!=null && !stack.isEmpty() && this.items.size()<16) {
			this.items.add(stack);
		}
	}
	public ItemStack setItem(int slot, ItemStack newItem) {
		if(this.items!=null && slot >= 0 && slot < this.items.size()) {
			return this.items.set(slot, newItem);
		}
		return null;
	}
	
	public void addMessage(String msg, Object... args) {
		if(this.msgs==null) this.msgs=Lists.newArrayList();
		if(msg!=null && !msg.isEmpty()) {
			this.msgs.add(new InboxText(msg, args));
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
	
	public Email copy() {
		Email copy = new Email(this.title.copy(), this.sender.copy(), null, null, null);
		if(this.hasSound()) {
			copy.sound = this.sound.copy();
		}
		if(this.hasItems()) {
			copy.items = Lists.newArrayList(this.items);
		}
		if(this.hasMessages()){
			copy.msgs = Lists.newArrayList(this.msgs);
		}
		return copy;
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		
		json.add("title", this.title.writeToJson());
		json.addProperty("time", this.create_time);
		if(this.expiration_time!=null) {
			json.addProperty("expiration", this.expiration_time.millis);
		}
		json.add("sender", this.sender.writeToJson());
		if(read) json.addProperty("read", true);
		if(accept) json.addProperty("accept", true);
		if(this.hasSound()) json.add("sound", this.sound.write(new JsonObject()));
		if(this.hasItems()) json.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		
		if(this.hasMessages()) {
			JsonObject msgs = new JsonObject();
			for(int i = 0; i < this.msgs.size(); i++) {
				IInboxText msg = this.msgs.get(i);
				if(msg.getParameters()!=null && msg.getParameters().length>0) {
					JsonArray arg = new JsonArray();
					for(int j = 0; j < msg.getParameters().length; j++) {
						arg.add(String.valueOf(msg.getParameters()[j]));
					}
					msgs.add(msg.getText(), arg);
				}else {
					msgs.add(msg.getText(), JsonNull.INSTANCE);
				}
			}
			json.add("msgs", msgs);
		}
		return json;
	}
	
	public static final SimpleDateFormat old_dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			this.title = new InboxText(json.get("title"));
			JsonElement time = json.get("time");
			if(time.isJsonPrimitive()) {
				JsonPrimitive p = time.getAsJsonPrimitive();
				if(p.isString()) {
					try {
						this.create_time = old_dateFormat.parse(p.getAsString()).getTime();
					}catch(Exception e) {
						e.printStackTrace();
						this.create_time = System.currentTimeMillis();
					}
				}else if(p.isNumber()) {
					this.create_time = p.getAsLong();
				}
			}
			if(json.has("expiration")) {
				this.expiration_time = new TimeMillis(json.get("expiration").getAsLong());
				this.expiration_time_l = -404;
			}
			this.sender = new InboxText(json.get("sender"));
			if(json.has("read")) this.read = true;
			if(json.has("accept")) this.accept = true;
			if(json.has("sound")) this.sound = new InboxSound(json.get("sound").getAsJsonObject());
			if(json.has("items")) this.items = JsonToStackUtil.toStacks(json.get("items"));
			
			if(json.has("msgs")) {
				this.msgs = Lists.newArrayList();
				JsonElement msgElement = json.get("msgs");
				if(msgElement.isJsonObject()) {
					JsonObject msgs = msgElement.getAsJsonObject();
					for(Entry<String, JsonElement> msg : msgs.entrySet()) {
						String key = msg.getKey();
						JsonElement a = msg.getValue();
						if(a.isJsonArray()) {
							JsonArray argJson = a.getAsJsonArray();
							Object[] args = new Object[argJson.size()];
							for(int i = 0; i < args.length; i++) {
								args[i] = argJson.get(i).getAsString();
							}
							this.msgs.add(new InboxText(key, args));
						}else {
							this.msgs.add(new InboxText(key));
						}
					}
				}else if(msgElement.isJsonArray()) {
					JsonArray msgs = msgElement.getAsJsonArray();
					for(int i = 0; i < msgs.size(); i++) {
						this.msgs.add(new InboxText(msgs.get(i).getAsString()));
					}
				}
			}
		}
	}

	protected static NBTBase emptyTag = null;
	public static NBTBase getEmptyTag() {
		if(emptyTag==null) {
			emptyTag = new NBTTagByte((byte)0);
		}
		return emptyTag;
	}
	
	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		
		nbt.setTag("title", this.title.writeToNBT());
		nbt.setLong("time", this.create_time);
		if(this.expiration_time!=null) {
			nbt.setLong("expiration", this.expiration_time.millis);
		}
		nbt.setTag("sender", this.sender.writeToNBT());
		if(read) nbt.setBoolean("read", true);
		if(accept) nbt.setBoolean("accept", true);
		if(this.hasSound()) nbt.setTag("sound", this.sound.write(new NBTTagCompound()));
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
				IInboxText msg = this.msgs.get(i);
				NBTBase msgNBT;
				if(msg.getParameters()!=null && msg.getParameters().length>0) {
					NBTTagCompound msgNBT0 = new NBTTagCompound();
					NBTTagList args = new NBTTagList();
					for(int j = 0; j < msg.getParameters().length; j++) {
						args.appendTag(new NBTTagString(String.valueOf(msg.getParameters()[i])));
					}
					msgNBT0.setString("text", msg.getText());
					msgNBT0.setTag("args", args);
					msgNBT = msgNBT0;
				}else if(!"".equals(msg.getText())) {
					msgNBT = new NBTTagString(msg.getText());
				}else {
					msgNBT = getEmptyTag();
				}
				msgs.setTag(String.valueOf(i), msgNBT);
			}
			nbt.setTag("msgs", msgs);
		}
		
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			this.title = new InboxText(nbt.getTag("title"));
			this.create_time = nbt.getLong("time");
			if(nbt.hasKey("expiration")) {
				this.expiration_time = new TimeMillis(nbt.getLong("expiration"));
				this.expiration_time_l = -404;
			}
			this.sender = new InboxText(nbt.getTag("sender"));
			if(nbt.hasKey("read")) this.read = true;
			if(nbt.hasKey("accept")) this.accept = true;
			if(nbt.hasKey("sound")) this.sound = new InboxSound(nbt.getCompoundTag("sound"));
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
				List<String> keys = msgs.getKeySet().stream().sorted((key0, key1)->Integer.valueOf(key0).compareTo(Integer.valueOf(key1))).collect(Collectors.toList());
				keys.forEach(key->{
					NBTBase msg = msgs.getTag(key);
					if(msg instanceof NBTTagString) {
						this.msgs.add(new InboxText(((NBTTagString) msg).getString()));
					}else if(msg instanceof NBTTagCompound) {
						NBTTagCompound text = (NBTTagCompound) msg;
						NBTTagList argsNBT = text.getTagList("args", 8);
						Object[] args = new Object[argsNBT.tagCount()];
						for(int i = 0; i < args.length; i++) {
							args[i] = argsNBT.getStringTagAt(i);
						}
						this.msgs.add(new InboxText(text.getString("text"), args));
					}else if(msg instanceof NBTTagByte) {
						this.msgs.add(InboxText.empty);
					}
				});
			}
		}
	}
	
	@Override
	public SQLValues write(SQLValues value) {
		return value;
	}

	@Override
	public void read(ResultSet result) throws SQLException {}
	
	@Override
	public String toString() {
		return this.writeTo(JsonObject.class).toString();
	}
	
	@Override
	public Email clone() {
		return this.copy();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (accept ? 1231 : 1237);
		result = prime * result + (int) (create_time ^ (create_time >>> 32));
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		result = prime * result + ((msgs == null) ? 0 : msgs.hashCode());
		result = prime * result + (read ? 1231 : 1237);
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + ((sound == null) ? 0 : sound.hashCode());
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
		if(create_time != other.create_time)
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
		if(title == null) {
			if(other.title != null)
				return false;
		}else if(!title.equals(other.title))
			return false;
		return true;
	}
	
}