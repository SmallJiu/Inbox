package cat.jiu.email.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;

public final class Inbox implements ISerializable {
	private static final HashMap<String, Inbox> inboxCache = Maps.newHashMap();
	
	/** all custom value will serialize to string */
	private final HashMap<String, Object> customValue = Maps.newHashMap();
	private final ArrayList<Email> emails = Lists.newArrayList();
	private final String owner;
	private boolean dev;
	
	private Inbox(UUID owner, NBTTagCompound inboxTag) {
		this.owner = owner.toString();
		this.read(inboxTag);
	}
	private Inbox(UUID owner, JsonObject inboxJson) {
		this(owner.toString(), inboxJson);
	}
	private Inbox(String owner, JsonObject inboxJson) {
		this.owner = owner;
		this.read(inboxJson);
	}
	/**
	 * @return true if this email list contains no emails.
	 */
	public boolean isEmptyEmails() {return this.emails.isEmpty();}
	/**
	 * @return true if this custom value list contains no values.
	 */
	public boolean isEmptyCustomValues() {return this.customValue.isEmpty();}
	/**
	 * @return the inbox owner string
	 */
	public String getOwner() {return owner;}
	/**
	 * @return the owner uuid
	 */
	public UUID getOwnerAsUUID() {
		try {
			return UUID.fromString(owner);
		}catch(Exception e) {
			return EmailUtils.getUUID(owner);
		}
	}
	/**
	 * @return emails count
	 */
	public int emailCount() {return emails.size();}
	/**
	 * @return custom values count
	 */
	public int customValueCount() {return customValue.size();}
	/**
	 * @return true if inbox has dev msg
	 */
	public boolean isSendDevMsg() {return dev;}
	/**
	 * set inbox dev msg state
	 */
	public void setSendDevMsg(boolean dev) {this.dev = dev;}
	/**
	 * @return inbox serialize size, for send network pack
	 */
	public long getInboxSize() {
		return EmailUtils.getSize(this.write(new NBTTagCompound()));
	}
	/**
	 * get inbox unread email count
	 * @return unread count
	 */
	public int getUnRead() {
		int i = 0;
		for(Email email : emails) {
			if(!email.isRead()) i++;
		}
		return i;
	}
	/**
	 * get inbox unreceived email count
	 * @return unreceived count
	 */
	public int getUnReceived() {
		int i = 0;
		for(Email email : emails) {
			if(email.hasItems() && !email.isReceived()) i++;
		}
		return i;
	}
	/**
	 * @param id email id
	 * @return true if inbox has email by id
	 */
	public boolean hasEmail(int id) {
		int size = this.emails!=null ? this.emails.size() : 0;
		return size > 0 && id < size && id >=0;
	}
	/**
	 * get email from id
	 * @param id email id
	 * @return emial by id
	 */
	public Email getEmail(int id) {
		return this.hasEmail(id) ? this.emails.get(id) : null;
	}
	/**
	 * remove email by id
	 * @return the id pos old email
	 */
	public Email deleteEmail(int id) {
		return this.hasEmail(id) ? this.emails.remove(id) : null;
	}
	/**
	 * set new email to id
	 * @param id 
	 * @param newEmail 
	 * @return the old email
	 */
	public Email setEmail(int id, Email newEmail) {
		return this.hasEmail(id) ? this.emails.set(id, newEmail) : null;
	}
	/**
	 * add email to inbox
	 * @return true if add email success
	 */
	public boolean addEmail(Email email) {
		return this.emails.add(email);
	}
	
	/**
	 * add custom value to inbox
	 */
	public void addCustom(String key, Object value) {
		this.customValue.put(key, value);
	}
	/**
	 * get custom value from inbox
	 */
	public Object getCustom(String key) {
		return this.customValue.get(key);
	}
	/**
	 * remove custom value from inbox
	 */
	public Object removeCustom(String key) {
		return this.customValue.remove(key);
	}
	/**
	 * @return true if inbox has {@code key} custom value
	 */
	public boolean hasCustomValue(String key) {
		return this.customValue.containsKey(key);
	}
	
	/**
	 * save inbox to disk
	 * @return true if save success
	 */
	public boolean save() {
		return JsonUtil.toJsonFile(EmailUtils.getSaveEmailPath() + owner + ".json", this.writeTo(JsonObject.class), false);
	}
	
	/**
	 * read inbox from disk
	 */
	public Inbox read() {
		this.emails.clear();
		this.customValue.clear();
		this.dev = false;
		this.readFrom(EmailUtils.getInboxJson(this.owner));
		return this;
	}
	
	// Serialize start
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		if(this.dev) json.addProperty("dev", true);
		if(!this.isEmptyEmails()) {
			JsonObject emails = new JsonObject();
			for(int i = 0; i < this.emails.size(); i++) {
				emails.add(Integer.toString(i), this.emails.get(i).writeTo(JsonObject.class));
			}
			json.add("emails", emails);
		}
		if(!this.isEmptyCustomValues()) {
			JsonObject customObj = new JsonObject();
			for(Entry<String, Object> custom : this.customValue.entrySet()) {
				Object value = custom.getValue();
				if(value instanceof Integer) {
					customObj.addProperty(custom.getKey(), (Integer)value);
				}else if(value instanceof Boolean) {
					customObj.addProperty(custom.getKey(), (Boolean)value);
				}else {
					customObj.addProperty(custom.getKey(), String.valueOf(value));
				}
			}
			json.add("custom", customObj);
		}
		return json;
	}

	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			if(json.has("dev")) {
				this.dev = json.get("dev").getAsBoolean();
			}
			if(json.has("custom")) {
				for(Entry<String, JsonElement> custom : json.getAsJsonObject("custom").entrySet()) {
					JsonElement value = custom.getValue();
					if(value.isJsonPrimitive()) {
						JsonPrimitive primitive = value.getAsJsonPrimitive();
						if(primitive.isBoolean()) {
							this.customValue.put(custom.getKey(), primitive.getAsBoolean());
						}else if(primitive.isNumber()) {
							this.customValue.put(custom.getKey(), primitive.getAsNumber());
						}else {
							this.customValue.put(custom.getKey(), primitive.getAsString());
						}
					}
				}
			}
			if(json.has("emails")) {
				JsonObject emails = json.getAsJsonObject("emails");
				for(Entry<String, JsonElement> email : emails.entrySet()) {
					this.emails.add(new Email(email.getValue().getAsJsonObject()));
				}
			}else {// for old version
				for(Entry<String, JsonElement> emails : json.entrySet()) {
					if(emails.getKey().equalsIgnoreCase("dev")) { continue;
					}else if(emails.getKey().equalsIgnoreCase("custom")) { continue;
					}else {
						this.emails.add(new Email(emails.getValue().getAsJsonObject()));
					}
				}
			}
		}
	}

	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		if(this.dev) nbt.setBoolean("dev", true);
		if(!this.isEmptyEmails()) {
			NBTTagCompound emails = new NBTTagCompound();
			for(int i = 0; i < this.emails.size(); i++) {
				emails.setTag(Integer.toString(i), this.emails.get(i).writeTo(NBTTagCompound.class));
			}
			nbt.setTag("emails", emails);
		}
		if(!this.isEmptyCustomValues()) {
			NBTTagCompound cutsomTag = new NBTTagCompound();
			for(Entry<String, Object> custom : this.customValue.entrySet()) {
				Object value = custom.getValue();
				if(value instanceof Integer) {
					cutsomTag.setInteger(custom.getKey(), (Integer) value);
				}else if(value instanceof Boolean) {
					cutsomTag.setBoolean(custom.getKey(), (Boolean) value);
				}else {
					cutsomTag.setString(custom.getKey(), String.valueOf(value));
				}
			}
			nbt.setTag("custom", cutsomTag);
		}
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			for(String email : nbt.getKeySet()) {
				if(email.equalsIgnoreCase("dev")) {
					this.dev = nbt.getBoolean(email);
					continue;
				}else if(email.equalsIgnoreCase("custom")) {
					NBTTagCompound customTag = nbt.getCompoundTag(email);
					for(String custom : customTag.getKeySet()) {
						NBTBase customValue = customTag.getTag(custom);
						if(customValue instanceof NBTTagByte) {
							this.customValue.put(custom, ((NBTTagByte)customValue).getByte()==1);
						}else if(customValue instanceof NBTTagInt) {
							this.customValue.put(custom, ((NBTTagInt)customValue).getInt());
						}else {
							this.customValue.put(custom, customValue.toString());
						}
					}
					continue;
				}else if(email.equalsIgnoreCase("emails")) {
					NBTTagCompound emailTag = nbt.getCompoundTag(email);
					if(emailTag.getSize()>0) {
						for(String emailKey : emailTag.getKeySet()) {
							this.emails.add(new Email(emailTag.getCompoundTag(emailKey)));
						}
					}
				}
			}
		}
	}
	// Serialize end
	
	@Override
	public String toString() {
		return this.write(new JsonObject()).toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customValue == null) ? 0 : customValue.hashCode());
		result = prime * result + (dev ? 1231 : 1237);
		result = prime * result + ((emails == null) ? 0 : emails.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		Inbox other = (Inbox) obj;
		if(customValue == null) {
			if(other.customValue != null)
				return false;
		}else if(!customValue.equals(other.customValue))
			return false;
		if(dev != other.dev)
			return false;
		if(emails == null) {
			if(other.emails != null)
				return false;
		}else if(!emails.equals(other.emails))
			return false;
		if(owner == null) {
			if(other.owner != null)
				return false;
		}else if(!owner.equals(other.owner))
			return false;
		return true;
	}
	
	/**
	 * @return the player inbox
	 */
	public static Inbox get(@Nonnull EntityPlayer player) {
		return get(player.getUniqueID());
	}
	/**
	 * @return the uuid inbox
	 */
	public static Inbox get(@Nonnull UUID uid) {
		return get(uid.toString());
	}
	/**
	 * @return the owner inbox
	 */
	public static Inbox get(@Nonnull String owner) {
		if(inboxCache.containsKey(owner)) {
			return inboxCache.get(owner).read();
		}
		Inbox inbox = new Inbox(owner, EmailUtils.getInboxJson(owner));
		inboxCache.put(owner, inbox);
		return inbox;
	}
	/**
	 * get inbox from nbt
	 * @param uid the owner
	 * @param inboxTag the inbox serialize nbt
	 * @return the inbox
	 */
	public static Inbox get(@Nonnull UUID uid, NBTTagCompound inboxTag) {
		if(inboxTag==null)return null;
		if(inboxCache.containsKey(uid.toString())) {
			Inbox inbox = inboxCache.get(uid.toString()).read();
			inbox.emails.clear();
			inbox.customValue.clear();
			inbox.dev = false;
			inbox.read(inboxTag);
			return inbox;
		}
		Inbox inbox = new Inbox(uid, inboxTag);
		inboxCache.put(uid.toString(), inbox);
		return inbox;
	}
	/**
	 * get inbox from json
	 * @param uid the owner
	 * @param inboxJson the inbox serialize json
	 * @return the inbox
	 */
	public static Inbox get(@Nonnull UUID uid, JsonObject inboxJson) {
		if(inboxJson==null)return null;
		if(inboxCache.containsKey(uid.toString())) {
			Inbox inbox = inboxCache.get(uid.toString());
			inbox.emails.clear();
			inbox.customValue.clear();
			inbox.dev = false;
			inbox.read(inboxJson);
			return inbox;
		}
		Inbox inbox = new Inbox(uid, inboxJson);
		inboxCache.put(uid.toString(), inbox);
		return inbox;
	}
}
