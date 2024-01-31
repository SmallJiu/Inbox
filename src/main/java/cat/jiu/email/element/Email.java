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

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.element.Sound;
import cat.jiu.core.util.element.Text;
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
	protected IText title;
	
	protected long create_time = System.currentTimeMillis();
	protected String create_time_s;
	
	protected TimeMillis expiration_time;
	protected long expiration_time_l;
	
	protected IText sender;
	protected ISound sound;
	protected List<ItemStack> items;
	protected List<IText> msgs;
	protected boolean read;
	protected boolean accept;
	
	public Email(IText title, IText sender) {
		this.title = title;
		this.sender = sender;
	}

	/**
	 * @param title the email title
	 * @param sender the send email sender
	 * @param sound the email sound
	 * @param items the email items
	 * @param msgs the email messages
	 * 
	 * @author small_jiu
	 */
	public Email(@Nonnull IText title, @Nonnull IText sender, ISound sound, List<ItemStack> items, List<IText> msgs) {
		this.title = title;
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
	/**
	 * @return 创建邮件的时间戳
	 */
	public long getCreateTimeAsTimestamp() {
		return create_time;
	}
	/**
	 * @return 创建邮件的时间，格式为( yyyy/MM/dd HH:mm:ss )
	 */
	public String getCreateTimeAsString() {
		if(this.create_time_s == null) {
			this.create_time_s = EmailUtils.dateFormat.format(new Date(this.create_time));
		}
		return this.create_time_s;
	}
	/**
	 * @return 邮件标题
	 */
	public IText getTitle() {return title;}
	/**
	 * @return 邮件发送者
	 */
	public IText getSender() {return sender;}
	/**
	 * @return 邮件附带的声音(音效)
	 */
	public ISound getSound() {return sound;}
	/**
	 * @return 邮件是否已读
	 */
	public boolean isRead() {return read;}
	/**
	 * @return 邮件是否已领
	 */
	public boolean isReceived() {return accept;}
	/**
	 * @return 邮件附带的物品
	 */
	public List<ItemStack> getItems() {return Lists.newArrayList(items);}
	/**
	 * @return 邮件附带的消息
	 */
	public List<IText> getMsgs() {return msgs;}
	
	protected long networkSize = -404;
	/**
	 * @return 邮件的网络包大小
	 */
	public long getEmailNetworkSize() {
		if(this.networkSize == -404) {
			this.networkSize = EmailUtils.getSize(this.writeTo(NBTTagCompound.class));
		}
		return this.networkSize;
	}

	/**
	 * @param sender 邮件新的发送者
	 */
	public Email setSender(IText sender) {this.sender = sender; return this;}
	/**
	 * @param title 邮件新的标题
	 */
	public Email setTitle(IText title) {this.title = title; return this;}
	/**
	 * @param read 邮件是否已读
	 */
	public Email setRead(boolean read) {this.read = read; return this;}
	/**
	 * @param accept 邮件是否已领
	 */
	public Email setAccept(boolean accept) {this.accept = accept; return this;}
	/**
	 * @param time 邮件的新的创建时间
	 * @deprecated {@link #setCreateTime(LocalDateTime)} 
	 */
	@Deprecated
	public void setTime(LocalDateTime time) {
		this.create_time = time.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		this.create_time_s = null;
	}
	/**
	 * @param time 邮件的新的创建时间
	 * @deprecated {@link #setCreateTime(Date)} 
	 */
	@Deprecated
	public void setTime(Date time) {
		this.create_time = time.getTime();
		this.create_time_s = null;
	}
	/**
	 * @param time 邮件的新的创建时间
	 */
	public Email setCreateTime(LocalDateTime time) {
		this.create_time = time.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		this.create_time_s = null;
		return this;
	}
	/**
	 * @param time 邮件的新的创建时间
	 */
	public Email setCreateTime(Date time) {
		this.create_time = time.getTime();
		this.create_time_s = null;
		return this;
	}
	/**
	 * 设置邮件的新的创建时间为系统当前时间
	 */
	public Email setCreateTimeToNow() {
		this.create_time = System.currentTimeMillis();
		this.create_time_s = null;
		return this;
	}
	
	/**
	 * @param expiration_time 邮件的新的过期时间
	 */
	public Email setExpirationTime(TimeMillis expiration_time) {
		if(this.expiration_time == null || this.expiration_time.millis == 0) {
			this.expiration_time = expiration_time;
		}
		return this;
	}
	/**
	 * @return 邮件的过期时间
	 */
	public TimeMillis getExpirationTime() {
		return expiration_time;
	}
	/**
	 * @return 邮件是否可过期
	 */
	public boolean hasExpirationTime() {
		return this.getExpirationTime() != null && this.getExpirationTime().millis > 0;
	}
	
	/**
	 * @return 邮件是否已过期
	 */
	public boolean isExpiration() {
		return this.isExpiration(System.currentTimeMillis());
	}
	/**
	 * @param time 时间戳
	 * @return 邮件是否已对提供的时间戳过期
	 */
	public boolean isExpiration(long time) {
		return this.getExpirationTime()!=null && time >= this.getExpirationTimeAsTimestamp();
	}
	
	/**
	 * @return 获取过期时间为时间戳
	 */
	public long getExpirationTimeAsTimestamp() {
		if(this.expiration_time_l == -404 || this.expiration_time_l <= 10 && this.expiration_time != null) {
			this.expiration_time_l = this.create_time + this.expiration_time.millis;
		}
		return this.expiration_time_l;
	}
	
	/**
	 * 设置邮件标题的附加内容
	 * @param index index
	 * @param obj obj
	 */
	public Email setTitleParameters(int index, Object obj) {
		this.getTitle().getParameters()[index] = obj;
		return this;
	}
	
	/**
	 * 设置邮件附加消息的附加内容
	 * @param msgIndex
	 * @param index
	 * @param obj
	 */
	public Email setMessageParameters(int msgIndex, int index, Object obj) {
		this.getMsgs().get(msgIndex).getParameters()[index] = obj;
		return this;
	}
	
	/**
	 * 设置邮件发送者的附加内容
	 * @param index
	 * @param obj
	 * @return
	 */
	public Email setSenderParameters(int index, Object obj) {
		this.getSender().getParameters()[index] = obj;
		return this;
	}
	
	/**
	 * 移除附加物品
	 * @param slot slot
	 * @return 已被移除的物品
	 */
	public ItemStack removeItem(int slot) {
		if(this.items!=null && slot >= 0 && slot < this.items.size()) {
			return this.items.remove(slot);
		}
		return null;
	}
	/**
	 * 清空邮件附带的所有物品
	 */
	public Email clearItems() {
		if(this.items!=null) {
			this.items.clear();
		}
		return this;
	}
	/**
	 * 添加物品到邮件，注：邮件最多只能有16个物品
	 * @param stacks
	 */
	public Email addItems(List<ItemStack> stacks) {
		if(this.items==null) this.items = Lists.newArrayList();
		if(stacks!=null && !stacks.isEmpty()) {
			for(int i = 0; i < stacks.size() && this.items.size()<16; i++) {
				this.items.add(stacks.get(i));
			}
		}
		return this;
	}
	/**
	 * 添加物品到邮件，注：邮件最多只能有16个物品
	 * @param stack
	 */
	public Email addItem(ItemStack stack) {
		if(this.items==null) this.items = Lists.newArrayList();
		if(stack!=null && !stack.isEmpty() && this.items.size()<16) {
			this.items.add(stack);
		}
		return this;
	}
	/**
	 * 设置邮件的附加物品
	 * @param slot
	 * @param newItem
	 */
	public ItemStack setItem(int slot, ItemStack newItem) {
		if(this.items!=null && slot >= 0 && slot < this.items.size()) {
			return this.items.set(slot, newItem);
		}
		return null;
	}
	
	/**
	 * 添加新消息到邮件
	 * @param msg
	 * @param args
	 */
	public Email addMessage(String msg, Object... args) {
		if(msg!=null && !msg.isEmpty()) {
			this.addMessage(new Text(msg, args));
		}else {
			this.addMessage(Text.empty);
		}
		return this;
	}
	
	/**
	 * 添加新消息到邮件
	 * @param text
	 */
	public Email addMessage(IText text) {
		if(this.msgs==null) this.msgs=Lists.newArrayList();
		
		if(text!=null) this.msgs.add(text);
		
		return this;
	}
	
	/**
	 * 设置邮件的附加消息
	 * @param index
	 * @param msg
	 * @param args
	 */
	public Email setMessage(int index, String msg, Object... args) {
		if(msg!=null && !msg.isEmpty()) {
			this.setMessage(index, new Text(msg, args));
		}
		return this;
	}
	
	/**
	 * 设置邮件的附加消息
	 * @param index
	 * @param text
	 */
	public Email setMessage(int index, IText text) {
		if(this.msgs==null || this.msgs.isEmpty()) return this;
		
		if(index >= 0 && index < this.msgs.size()) {
			this.msgs.set(index, text);
		}
		
		return this;
	}
	
	/**
	 * 移除邮件的附加消息
	 * @param index
	 */
	public IText removeMessage(int index) {
		if(this.msgs==null || this.msgs.isEmpty()) return null;
		if(index >= 0 && index < this.msgs.size()) {
			return this.msgs.remove(index);
		}
		return null;
	}
	
	/**
	 * @return 是否带有附加的声音(音效)
	 */
	public boolean hasSound() {
		return this.sound!=null && this.sound.getSound()!=null;
	}
	/**
	 * @return 是否带有附加的物品
	 */
	public boolean hasItems() {
		return this.items!=null && !this.items.isEmpty();
	}
	/**
	 * @return 是否带有附加的消息
	 */
	public boolean hasMessages() {
		return this.msgs!=null && !this.msgs.isEmpty();
	}
	
	/**
	 * @return 新的邮件对象
	 */
	public Email copy() {
		Email copy = new Email(this.getTitle().copy(), this.getSender().copy(), null, null, null);
		if(this.hasSound()) {
			copy.sound = this.getSound().copy();
		}
		if(this.hasItems()) {
			List<ItemStack> items = Lists.newArrayList();
			for(int i = 0; i < this.getItems().size(); i++) {
				items.add(this.getItems().get(i).copy());
			}
			copy.items = items;
		}
		if(this.hasMessages()){
			List<IText> msgs = Lists.newArrayList();
			for(int i = 0; i < this.getMsgs().size(); i++) {
				msgs.add(this.getMsgs().get(i).copy());
			}
			copy.msgs = msgs;
		}
		return copy;
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		
		json.add("title", this.title.writeTo(JsonObject.class));
		json.addProperty("time", this.create_time);
		if(this.expiration_time!=null) {
			json.addProperty("expiration", this.expiration_time.millis);
		}
		
		json.add("sender", this.sender.writeTo(JsonObject.class));
		if(read) json.addProperty("read", true);
		if(accept) json.addProperty("accept", true);
		if(this.hasSound()) json.add("sound", this.sound.write(new JsonObject()));
		if(this.hasItems()) json.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		
		if(this.hasMessages()) {
			JsonObject msgs = new JsonObject();
			for(int i = 0; i < this.msgs.size(); i++) {
				IText msg = this.msgs.get(i);
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
			if(json.get("title").isJsonObject()) {
				this.title = new Text(json.getAsJsonObject("title"));
			}else if(json.get("title").isJsonPrimitive()) {
				this.title = new Text(json.get("title").getAsString());
			}
			
			if(json.get("sender").isJsonObject()) {
				this.sender = new Text(json.getAsJsonObject("sender"));
			}else if(json.get("sender").isJsonPrimitive()) {
				this.sender = new Text(json.get("sender").getAsString());
			}
			
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
			if(json.has("read")) this.read = json.get("read").getAsBoolean();
			if(json.has("accept")) this.accept = json.get("accept").getAsBoolean();
			if(json.has("sound")) this.sound = new Sound(json.getAsJsonObject("sound"));
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
							this.msgs.add(new Text(key, args));
						}else {
							this.msgs.add(new Text(key));
						}
					}
				}else if(msgElement.isJsonArray()) {
					JsonArray msgs = msgElement.getAsJsonArray();
					for(int i = 0; i < msgs.size(); i++) {
						this.msgs.add(new Text(msgs.get(i).getAsString()));
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
		
		nbt.setTag("title", this.title.writeTo(NBTTagCompound.class));
		nbt.setLong("time", this.create_time);
		if(this.expiration_time!=null) {
			nbt.setLong("expiration", this.expiration_time.millis);
		}
		nbt.setTag("sender", this.sender.writeTo(NBTTagCompound.class));
		if(this.isRead()) nbt.setBoolean("read", this.isRead());
		if(this.isReceived()) nbt.setBoolean("accept", this.isReceived());
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
				IText msg = this.msgs.get(i);
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
			this.title = new Text(nbt.getCompoundTag("title"));
			this.create_time = nbt.getLong("time");
			if(nbt.hasKey("expiration")) {
				this.expiration_time = new TimeMillis(nbt.getLong("expiration"));
				this.expiration_time_l = -404;
			}
			this.sender = new Text(nbt.getCompoundTag("sender"));
			if(nbt.hasKey("read")) this.read = nbt.getBoolean("read");
			if(nbt.hasKey("accept")) this.accept = nbt.getBoolean("accept");
			if(nbt.hasKey("sound")) this.sound = new Sound(nbt.getCompoundTag("sound"));
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
						this.msgs.add(new Text(((NBTTagString) msg).getString()));
					}else if(msg instanceof NBTTagCompound) {
						NBTTagCompound text = (NBTTagCompound) msg;
						NBTTagList argsNBT = text.getTagList("args", 8);
						Object[] args = new Object[argsNBT.tagCount()];
						for(int i = 0; i < args.length; i++) {
							args[i] = argsNBT.getStringTagAt(i);
						}
						this.msgs.add(new Text(text.getString("text"), args));
					}else if(msg instanceof NBTTagByte) {
						this.msgs.add(Text.empty);
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