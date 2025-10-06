package cat.jiu.email.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.configs.EmailConfigServer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;

import cat.jiu.email.EmailMain;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.sql.SQLValues;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;

public final class Inbox implements IDataSerializable<IData.IMapData<?>> {
	private static final HashMap<String, Inbox> inboxCache = Maps.newHashMap();
	
	/** all custom value will serialize to string */
	private final HashMap<String, Object> customValue = Maps.newHashMap();
	private final LinkedHashMap<Long, Email> emails = Maps.newLinkedHashMap();
	private final ArrayList<String> senderBlacklist = Lists.newArrayList();
	private final String owner;
	private boolean dev;
	private long emailHistoryCount = 0;
	private long sendCooling;

	private Inbox(String owner) {
		this.owner = owner;
	}
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
		CompoundTag nbt = (CompoundTag) this.write(NBTData.map()).getData();
		
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
		for(long id : this.getEmailIDs()) {
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
		for(long id : this.getEmailIDs()) {
			Email email = this.getEmail(id);
			if(email.hasAttachments() && !email.isReceived()) i++;
		}
		return i;
	}

	public long getEmailHistoryCount() {
		return emailHistoryCount;
	}

	/**
	 * @param id email id
	 * @return true if inbox has email with id
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
		return this.hasEmail(id) ? this.emails.get(id) : Email.EMPTY;
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
	public Inbox deleteAllEmail(){
		this.emails.clear();
		return this;
	}
	/**
	 * set new email to id
	 * @return the previous email associated with id
	 */
	public Email setEmail(long id, Email newEmail) {
		return this.emails.put(id, newEmail);
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
		if(this.setEmail(id, email) == null) {
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

	public long getSendCooling() {
		return sendCooling;
	}
	public Inbox setSendCooling(long sendCooling) {
		this.sendCooling = sendCooling;
		return this;
	}
	public Inbox setSendCoolingFromTime(long coolingMillis) {
		this.sendCooling = System.currentTimeMillis() + coolingMillis;
		return this;
	}
	public Inbox setSendCoolingToNow() {
		this.sendCooling = System.currentTimeMillis() + EmailConfigServer.Send.cooling.getMillis();
		return this;
	}
	public boolean isSendCooling(){
		return System.currentTimeMillis() <= this.sendCooling;
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
	 */
	public List<String> getSenderBlacklist() {
		return senderBlacklist;
	}
	
	/**
	 * save inbox to disk
	 * @return true if save success
	 */
	public boolean saveToDisk() {
		if(SideProxy.isClient()
		&& !Minecraft.getInstance().isLocalServer()) {
			EmailMain.log.error("Client can not save inbox to Server!");
			return false;
		}
		try {
			StorageType type = StorageType.getInstance();
			if (type!=null) {
				type.write.accept(this);
				return true;
			}
		} catch (Exception e) {
			EmailMain.log.error(e);
		}
		return false;
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
		this.read(JsonData.map(json));
		return this;
	}
	private Inbox readFromDisk(CompoundTag nbt) {
		this.emails.clear();
		this.customValue.clear();
		this.senderBlacklist.clear();
		this.dev = false;
		this.read(NBTData.map(nbt));
		return this;
	}
	
	// Serialize start

	@Override
	public IData.IMapData<?> write(IData.IMapData<?> data) {
		if(this.dev) data.putData("dev", true);

		data.putData("historySize", this.emailHistoryCount > 0 && this.emailHistoryCount > this.emails.size() ? this.emailHistoryCount : this.emails.size());
		data.putData("sendCooling", this.sendCooling);

		if(!this.isEmptyInbox()) {
			IData.IMapData<?> emails = data.newMap();
			for (long id : this.getEmailIDs()) {
				Email email =  this.getEmail(id);
				if (!email.isEmpty()) {
					emails.putData(String.valueOf(id), email.write(data.newMap()));
				}
			}
			data.putData("emails", emails);
		}

		if(!this.isEmptyCustomValues()) {
			IData.IMapData<?> customTag = data.newMap();
			for(Entry<String, Object> custom : this.customValue.entrySet()) {
				Object value = custom.getValue();
				if(value instanceof Integer) {
					customTag.putData(custom.getKey(), (Integer) value);
				}else if(value instanceof Boolean) {
					customTag.putData(custom.getKey(), (Boolean) value);
				}else {
					customTag.putData(custom.getKey(), String.valueOf(value));
				}
			}
			data.putData("custom", customTag);
		}

		if(!this.senderBlacklist.isEmpty()) {
			data.putData("blacklist", this.senderBlacklist.toArray(new String[0]));
		}
		return data;
	}

	private static final List<String> old_version_black_key = Arrays.asList("dev", "custom", "historySize", "blacklist", "sendCooling");
	@Override
	public void read(IData.IMapData<?> data) {
		if(data!=null && !data.isEmpty()) {
			if(data.containsKey("dev")) this.dev = data.getBoolean("dev");
			this.sendCooling = data.getLong("sendCooling");

			if(data.containsKey("custom")) {
				data.getMap("custom").foreach((key, value) -> {
					IData.IPrimitiveData<?> primitive = data.getAsPrimitive();
					if(primitive.isNumber()) {
						Number number = primitive.getAsNumber();
						if (number instanceof Byte) {
							if (primitive.getAsByte() == 1 || primitive.getAsByte() == 0) {
								this.customValue.put(key, primitive.getAsByte()==1);
							}else {
								this.customValue.put(key, primitive.getAsByte());
							}
						}else if (number instanceof Integer) {
							this.customValue.put(key, primitive.getAsInt());
						}
					}else {
						this.customValue.put(key, primitive.getAsString());
					}
				});
			}

			if(data.containsKey("emails")) {
				data.getMap("emails").foreach(((key, value) ->
					this.emails.put(Long.valueOf(key), new Email(value.getAsMap()))
				));
			}else {// for old version
				data.foreach((key, value) -> {
					try {
						this.emails.put(Long.valueOf(key), new Email(value.getAsMap()));
					} catch (Exception ignored) {}
				});
			}

			this.emailHistoryCount = this.emails.size();
			if(data.containsKey("historySize")) {
				long historySize = data.getLong("historySize");
				if(historySize > this.emails.size()) {
					this.emailHistoryCount = historySize;
				}
			}
			long emailMaxID = 0;
			for(Entry<Long, Email> id : this.emails.entrySet()) {
				emailMaxID = Math.max(emailMaxID, id.getKey());
			}
			this.emailHistoryCount = Math.max(this.emailHistoryCount, emailMaxID);

			data.getList("blacklist", String.class).foreach((index, value)->{
				String name = value.getAsPrimitive().getAsString();
				if(!this.isInSenderBlacklist(name)) {
					this.addSenderBlacklist(name);
				}
			});
		}
	}

	public JsonObject write(JsonObject json) {
		this.write(JsonData.map(json));
		return json;
	}

	public void read(JsonObject json) {
		this.read(JsonData.map(json));
	}

	public CompoundTag write(CompoundTag data) {
		this.write(NBTData.map(data));
		return data;
	}

	public void read(CompoundTag data) {
		this.read(NBTData.map(data));
	}

	public SQLValues write(SQLValues value) {
		if(value==null) value = new SQLValues();
		value.put("uuid", "'" + this.getOwner() + "'");
		value.put("inbox", "'" + this.write(new JsonObject()) + "'");
		return value;
	}

	public void read(ResultSet result) throws SQLException {
		if(result.next()) {
			this.read(JsonParser.parseString(result.getString("inbox")).getAsJsonObject());
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
			return other.senderBlacklist == null;
		}else return senderBlacklist.equals(other.senderBlacklist);
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

	public static Inbox getEmpty(String owner) {
		return new Inbox(owner);
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
		if(SideProxy.isServerClosed()) {
			inboxCache.clear();
		}
	}
}
