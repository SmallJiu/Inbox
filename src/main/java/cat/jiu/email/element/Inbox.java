package cat.jiu.email.element;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public final class Inbox implements INBTSerializable, IJsonSerializable {
	protected final UUID owner;
	protected ArrayList<Email> emails = Lists.newArrayList();
	protected boolean dev;
	
	public Inbox(UUID owner, JsonObject inboxJson) {
		this.owner = owner;
		this.read(inboxJson);
	}
	
	public Inbox(UUID owner, NBTTagCompound inboxTag) {
		this.owner = owner;
		this.read(inboxTag);
	}
	
	public UUID getOwner() {return owner;}
	public int count() {return emails.size();}
	public boolean isSendDevMsg() {return dev;}
	public void setSendDevMsg(boolean dev) {this.dev = dev;}
	public long getInboxSize() {return EmailUtils.getEmailSize(this.write(new NBTTagCompound()));}
	public int getUnRead() {
		int i = 0;
		for(Email email : emails) {
			if(!email.isRead()) i++;
		}
		return i;
	}
	public int getUnReceived() {
		int i = 0;
		for(Email email : emails) {
			if(email.hasItems() && !email.isReceived()) i++;
		}
		return i;
	}
	
	public boolean has(int id) {
		int size = this.emails.size();
		return id < size && id >=0;
	}
	public Email get(int id) {
		if(!this.has(id)) return null;
		return this.emails.get(id);
	}
	public Email delete(int id) {
		return this.emails.remove(id);
	}
	public Email set(int id, Email newEmail) {
		return this.emails.set(id, newEmail);
	}
	public boolean add(Email email) {
		return this.emails.add(email);
	}
	
	public void save() {
		JsonUtil.toJsonFile(EmailUtils.getSaveEmailPath() + owner + ".json", this.write(new JsonObject()), false);
	}
	
	public void read() {
		this.read(EmailUtils.getInboxJson(this.owner.toString()));
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		if(this.dev) json.addProperty("dev", true);
		for(int i = 0; i < this.emails.size(); i++) {
			json.add(Integer.toString(i), this.emails.get(i).write(new JsonObject()));
		}
		return json;
	}

	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			if(json.has("dev")) this.dev = true;
			for(Entry<String, JsonElement> emails : json.entrySet()) {
				if(!emails.getKey().equalsIgnoreCase("dev")) {
					this.emails.add(new Email(emails.getValue().getAsJsonObject()));
				}
			}
		}
	}

	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		if(this.dev) nbt.setBoolean("dev", true);
		for(int i = 0; i < this.emails.size(); i++) {
			nbt.setTag(Integer.toString(i), this.emails.get(i).write(new NBTTagCompound()));
		}
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			if(nbt.hasKey("dev")) this.dev = true;
			for(String email : nbt.getKeySet()) {
				if(!email.equalsIgnoreCase("dev")) {
					this.emails.add(new Email(nbt.getCompoundTag(email)));
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return this.write(new JsonObject()).toString();
	}
	
	public static Inbox get(EntityPlayer player) {
		return get(player.getUniqueID());
	}
	public static Inbox get(UUID uid) {
		return new Inbox(uid, EmailUtils.getInboxJson(uid.toString()));
	}
}
