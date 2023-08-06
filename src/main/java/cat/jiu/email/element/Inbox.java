package cat.jiu.email.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.JsonParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.email.EmailMain;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.sql.SQLValues;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;

public final class Inbox implements ISerializable {
	private static final HashMap<String, Inbox> inboxCache = Maps.newHashMap();
	
	/** all custom value will serialize to string */
	private final HashMap<String, Object> customValue = Maps.newHashMap();
	private final LinkedHashMap<Long, Email> emails = Maps.newLinkedHashMap();
	private final ArrayList<String> senderBlacklist = Lists.newArrayList();
	private final String owner;
	private boolean dev;
	private long emailHistoryCount = 0;
	
	private Inbox(String owner, CompoundTag inboxTag) {
		this.owner = owner;
		this.read(inboxTag);
	}
	private Inbox(String owner, JsonObject inboxJson) {
		this.owner = owner;
		this.read(inboxJson);
	}
	private Inbox(String owner, ResultSet set) throws SQLException {
		this.owner = owner;
		this.read(set);
	}
	/**
	 * @return emails id
	 */
	public synchronized Set<Long> getEmailIDs() {
		LinkedHashSet<Long> ids = Sets.newLinkedHashSet();
		
		Throwable exception;
		do {
			try {
				Set<Long> remove = Sets.newHashSet();
				this.emails.keySet().forEach(e->{
					if(this.getEmail(e)==null) {
						remove.add(e);
					}else {
						ids.add(e);
					}
				});
				remove.forEach(this::deleteEmail);
				exception = null;
			}catch(Throwable e) {
				exception = e;
			}
		}while(exception != null);
		
		return ids.stream().sorted(Long::compare).collect(Collectors.toCollection(LinkedHashSet::new));
	}
	/**
	 * @return true if this email list contains no emails.
	 */
	public boolean isEmptyInbox() {return this.emails.isEmpty();}
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
		CompoundTag nbt = this.writeTo(CompoundTag.class);
		
		nbt.remove("historySize");
		nbt.remove("blacklist");
		nbt.remove("dev");
		
		return EmailUtils.getSize(nbt);
	}
	/**
	 * get inbox unread email count
	 * @return unread count
	 */
	public int getUnRead() {
		int i = 0;
		for(long id : this.emails.keySet()) {
			if(!this.getEmail(id).isRead()) i++;
		}
		return i;
	}
	/**
	 * get inbox unreceived email count
	 * @return unreceived count
	 */
	public int getUnReceived() {
		int i = 0;
		for(long id : this.emails.keySet()) {
			Email email = this.getEmail(id);
			if(email.hasItems() && !email.isReceived()) i++;
		}
		return i;
	}
	/**
	 * @param id email id
	 * @return true if inbox has email by id
	 */
	public boolean hasEmail(long id) {
		return this.emails.containsKey(id);
	}
	/**
	 * get email from id
	 * @param id email id
	 * @return email by id
	 */
	public Email getEmail(long id) {
		return this.hasEmail(id) ? this.emails.get(id) : null;
	}
	/**
	 * remove email by id
	 * @return the previous email associated with id
	 */
	public synchronized Email deleteEmail(long id) {
		Email old = null;
		if(this.hasEmail(id)) {
			old = this.emails.remove(id);
		}
		return old;
	}
	/**
	 * set new email to id
	 * @param id 
	 * @param newEmail 
	 * @return the previous email associated with id
	 */
	public Email setEmail(long id, Email newEmail) {
		return this.hasEmail(id) ? this.emails.put(id, newEmail) : null;
	}
	/**
	 * add email, but not save inbox to disk.
	 * @param email the email
	 * @return the previous email associated with id
	 */
	public boolean addEmail(Email email) {
		return this.addEmail(email, false);
	}
	/**
	 * add email and save inbox to disk.
	 * @param email the email
	 * @param saveToDisk true if you want save inbox to disk
	 * @return the previous email associated with id
	 */
	public boolean addEmail(Email email, boolean saveToDisk) {
		long id = this.emailHistoryCount+1;
		if(this.emails.put(id, email) == null) {
			this.emailHistoryCount = id;
			return !saveToDisk || EmailUtils.saveInboxToDisk(this);
		}
		return false;
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
	 * @return inbox custom value
	 */
	public Map<String, Object> getCustomValue() {
		return Collections.unmodifiableMap(customValue);
	}
	
	/**
	 * add sender blacklist to inbox
	 * @param name the sender name
	 */
	public void addSenderBlacklist(String name) {
		this.senderBlacklist.add(name);
	}
	
	/**
	 * remove sender blacklist from inbox
	 * @param name the sender name
	 * @return true if remove success
	 */
	public boolean removeSenderBlacklist(String name) {
		return this.senderBlacklist.remove(name);
	}
	
	/**
	 * check sender name is in blacklist
	 * @param name the sender name
	 * @return true if name in blacklist
	 */
	public boolean isInSenderBlacklist(String name) {
		return this.senderBlacklist.contains(name);
	}
	
	/**
	 * get sender blacklist
	 * @return
	 */
	public List<String> getSenderBlacklist() {
		return Collections.unmodifiableList(senderBlacklist);
	}
	
	/**
	 * save inbox to disk
	 * @return true if save success
	 */
	public boolean saveToDisk() {
		if(EmailMain.proxy.isClient()
		&& !Minecraft.getInstance().isLocalServer()) {
			EmailMain.log.error("Client can not save inbox to Server!");
			return false;
		}
		if(this.isEmptyInbox()){
			EmailMain.log.error("Inbox is EMPTY! unknown bug for this. Inbox json: {}", this);
		}
		if(EmailConfigs.Save_Inbox_To_SQL.get()) {
			return EmailUtils.saveInboxToDB(this);
		}else {
			return JsonParser.toJsonFile(EmailAPI.getSaveInboxPath() + owner + ".json", this.writeTo(JsonObject.class), false);
		}
	}
	
	/**
	 * read inbox from disk
	 */
	public Inbox readFromDisk() {
		return this.readFromDisk(EmailUtils.getInboxJson(this.owner));
	}
	private Inbox readFromDisk(JsonObject json) {
		this.emails.clear();
		this.customValue.clear();
		this.senderBlacklist.clear();
		this.dev = false;
		this.readFrom(json);
		return this;
	}
	private Inbox readFromDisk(CompoundTag nbt) {
		this.emails.clear();
		this.customValue.clear();
		this.senderBlacklist.clear();
		this.dev = false;
		this.readFrom(nbt);
		return this;
	}
	
	// Serialize start
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		if(this.dev) json.addProperty("dev", true);
		json.addProperty("historySize", this.emailHistoryCount > 0 && this.emailHistoryCount > this.emails.size() ? this.emailHistoryCount : this.emails.size());
		
		if(!this.senderBlacklist.isEmpty()) {
			JsonArray list = new JsonArray();
			this.senderBlacklist.forEach(list::add);
			json.add("blacklist", list);
		}
		if(!this.isEmptyInbox()) {
			JsonObject emails = new JsonObject();
			for(Entry<Long, Email> email : this.emails.entrySet()) {
				emails.add(String.valueOf(email.getKey()), email.getValue().writeTo(JsonObject.class));
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

	static final List<String> old_version_black_key = Arrays.asList("dev", "custom", "historySize", "blacklist");

	@Override
	public void read(JsonObject json) {
//		EmailMain.log.error(String.valueOf(json));
		if(json!=null && json.size()>0) {
			if(json.has("dev")) this.dev = json.get("dev").getAsBoolean();
			
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
			if(json.has("blacklist")) {
				json.getAsJsonArray("blacklist").forEach(e->{
					String name = e.getAsString();
					if(!this.isInSenderBlacklist(name)) {
						this.addSenderBlacklist(name);
					}
				});
			}
			
			if(json.has("emails")) {
				JsonObject emails = json.getAsJsonObject("emails");
				for(Entry<String, JsonElement> email : emails.entrySet()) {
					this.emails.put(Long.valueOf(email.getKey()), new Email(email.getValue().getAsJsonObject()));
				}
			}else {// for old version
				for(Entry<String, JsonElement> emails : json.entrySet()) {
					if(!old_version_black_key.contains(emails.getKey())) {
						this.emails.put(Long.valueOf(emails.getKey()), new Email(emails.getValue().getAsJsonObject()));
					}
				}
			}
			
			this.emailHistoryCount = this.emails.size();
			if(json.has("historySize")) {
				long historySize = json.get("historySize").getAsLong();
				if(historySize > this.emails.size()) {
					this.emailHistoryCount = historySize;
				}
			}
			
			long emailMaxID = 0;
			for(Entry<Long, Email> id : this.emails.entrySet()) {
				emailMaxID = Math.max(emailMaxID, id.getKey());
			}
			this.emailHistoryCount = Math.max(this.emailHistoryCount, emailMaxID);
		}
	}

	@Override
	public CompoundTag write(CompoundTag nbt) {
		if(nbt==null) nbt = new CompoundTag();
		
		if(this.dev) nbt.putBoolean("dev", true);
		
		nbt.putLong("historySize", this.emailHistoryCount > 0 && this.emailHistoryCount > this.emails.size() ? this.emailHistoryCount : this.emails.size());
		
		if(!this.isEmptyInbox()) {
			CompoundTag emails = new CompoundTag();
			for(Entry<Long, Email> email : this.emails.entrySet()) {
				emails.put(String.valueOf(email.getKey()), email.getValue().writeTo(CompoundTag.class));
			}
			nbt.put("emails", emails);
		}
		
		if(!this.isEmptyCustomValues()) {
			CompoundTag customTag = new CompoundTag();
			for(Entry<String, Object> custom : this.customValue.entrySet()) {
				Object value = custom.getValue();
				if(value instanceof Integer) {
					customTag.putInt(custom.getKey(), (Integer) value);
				}else if(value instanceof Boolean) {
					customTag.putBoolean(custom.getKey(), (Boolean) value);
				}else {
					customTag.putString(custom.getKey(), String.valueOf(value));
				}
			}
			nbt.put("custom", customTag);
		}
		
		if(!this.senderBlacklist.isEmpty()) {
			ListTag list = new ListTag();
			this.senderBlacklist.forEach(s->
					list.add(StringTag.valueOf(s)));
			nbt.put("blacklist", list);
		}
		
		return nbt;
	}

	@Override
	public void read(CompoundTag nbt) {
		if(nbt!=null && !nbt.isEmpty()) {
			if(nbt.contains("dev")) this.dev = nbt.getBoolean("dev");
			
			if(nbt.contains("custom")) {
				CompoundTag customTag = nbt.getCompound("custom");
				for(String custom : customTag.getAllKeys()) {
					Tag customValue = customTag.get(custom);
					if(customValue instanceof ByteTag) {
						this.customValue.put(custom, ((ByteTag)customValue).getAsByte()==1);
					}else if(customValue instanceof IntTag) {
						this.customValue.put(custom, ((IntTag)customValue).getAsInt());
					}else {
						this.customValue.put(custom, customValue.toString());
					}
				}
			}
			
			if(nbt.contains("emails")) {
				CompoundTag emailTag = nbt.getCompound("emails");
				if(!emailTag.isEmpty()) {
					for(String emailKey : emailTag.getAllKeys()) {
						this.emails.put(Long.valueOf(emailKey), new Email(emailTag.getCompound(emailKey)));
					}
				}
			}
			
			this.emailHistoryCount = this.emails.size();
			if(nbt.contains("historySize")) {
				long historySize = nbt.getLong("historySize");
				if(historySize > this.emails.size()) {
					this.emailHistoryCount = historySize;
				}
			}
			long emailMaxID = 0;
			for(Entry<Long, Email> id : this.emails.entrySet()) {
				emailMaxID = Math.max(emailMaxID, id.getKey());
			}
			this.emailHistoryCount = Math.max(this.emailHistoryCount, emailMaxID);
			
			if(nbt.contains("blacklist")) {
				nbt.getList("blacklist", 8).forEach(s->{
					String name = s.getAsString();
					if(!this.isInSenderBlacklist(name)) {
						this.addSenderBlacklist(name);
					}
				});
			}
		}
	}
	
	@Override
	public SQLValues write(SQLValues value) {
		if(value==null) value = new SQLValues();
		value.put("uuid", "'" + this.getOwner() + "'");
		value.put("inbox", "'" + this.writeTo(JsonObject.class) + "'");
		return value;
	}
	
	@Override
	public void read(ResultSet result) throws SQLException {
		if(result.next()) {
			this.read(JsonParser.parser.parse(result.getString("inbox")).getAsJsonObject());
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
		result = prime * result + (int) (emailHistoryCount ^ (emailHistoryCount >>> 32));
		result = prime * result + ((emails == null) ? 0 : emails.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((senderBlacklist == null) ? 0 : senderBlacklist.hashCode());
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
		if(emailHistoryCount != other.emailHistoryCount)
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
		if(senderBlacklist == null) {
			if(other.senderBlacklist != null)
				return false;
		}else if(!senderBlacklist.equals(other.senderBlacklist))
			return false;
		return true;
	}
	
	/**
	 * @return the player inbox
	 */
	public static Inbox get(@Nonnull Player player) {
		return get(player.getUUID());
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
		Inbox inbox;
		if(inboxCache.containsKey(owner)) {
			inbox = inboxCache.get(owner).readFromDisk();
		}else {
			try {
				owner = UUID.fromString(owner).toString();
			}catch(Exception e) {
				if(EmailUtils.hasName(owner)) {
					owner = EmailUtils.getUUID(owner).toString();
				}
			}
			inbox = new Inbox(owner, EmailUtils.getInboxJson(owner));
			inboxCache.put(owner, inbox);
		}
		
		checkExpirationEmail(inbox);
		return inbox;
	}
	/**
	 * get inbox from nbt
	 * @param uid the owner
	 * @param inboxTag the inbox serialize nbt
	 * @return the inbox
	 */
	public static Inbox get(@Nonnull UUID uid, CompoundTag inboxTag) {
		if(inboxTag==null)return null;
		Inbox inbox;
		if(inboxCache.containsKey(uid.toString())) {
			inbox = inboxCache.get(uid.toString()).readFromDisk(inboxTag);
		}else {
			inbox = new Inbox(uid.toString(), inboxTag);
			inboxCache.put(uid.toString(), inbox);
		}
		checkExpirationEmail(inbox);
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
		
		Inbox inbox;
		if(inboxCache.containsKey(uid.toString())) {
			inbox = inboxCache.get(uid.toString()).readFromDisk(inboxJson);
		}else {
			inbox = new Inbox(uid.toString(), inboxJson);
			inboxCache.put(uid.toString(), inbox);
		}
		checkExpirationEmail(inbox);
		return inbox;
	}
	
	/**
	 * get inbox from json
	 * @param uid the owner
	 * @param set the inbox serialize data
	 * @return the inbox
	 * @throws SQLException 
	 */
	public static Inbox get(@Nonnull UUID uid, ResultSet set) throws SQLException {
		if(set==null)return null;
		
		Inbox inbox;
		if(inboxCache.containsKey(uid.toString())) {
			inbox = inboxCache.get(uid.toString());
			inbox.read(set);
		}else {
			inbox = new Inbox(uid.toString(), set);
			inboxCache.put(uid.toString(), inbox);
		}
		checkExpirationEmail(inbox);
		return inbox;
	}
	
	public static void checkExpirationEmail(Inbox inbox) {
		long sys = System.currentTimeMillis();
		boolean hasExpiration = false;
		for(Long id : inbox.getEmailIDs()) {
			if(inbox.getEmail(id).isExpiration(sys)) {
				inbox.deleteEmail(id);
				hasExpiration = true;
			}
		}
		if(hasExpiration) {
			EmailUtils.saveInboxToDisk(inbox);
		}
	}
	/**
	 * Do not use it anywhere except Server Stopped
	 */
	public static void clearCache() {
		if(EmailMain.proxy.isServerClosed()) {
			inboxCache.clear();
		}
	}
}
