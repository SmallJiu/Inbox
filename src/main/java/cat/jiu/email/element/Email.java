package cat.jiu.email.element;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.email.util.JsonToStackUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public final class Email implements INBTSerializable, IJsonSerializable {
	protected String title;
	protected String time;
	protected String sender;
	protected EmailSound sound;
	protected List<ItemStack> items;
	protected List<String> msgs;
	protected boolean read;
	protected boolean accept;
	
	/**
	 * @param title the email title
	 * @param time the email send time
	 * @param sender the send email sender
	 * @param sound the email sound
	 * @param items the email items
	 * @param msgs the email messages
	 * 
	 * @author small_jiu
	 */
	public Email(String title, String time, String sender, EmailSound sound, List<ItemStack> items, List<String> msgs) {
		this.title = title;
		this.time = time;
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
	public String getTitle() {return title;}
	public String getSender() {return sender;}
	public EmailSound getSound() {return sound;}
	public boolean isRead() {return read;}
	public boolean isReceived() {return accept;}
	public List<ItemStack> getItems() {return items;}
	public List<String> getMsgs() {return msgs;}

	public void setSender(String sender) {this.sender = sender;}
	public void setTitle(String title) {this.title = title;}
	public void setRead(boolean read) {this.read = read;}
	public void setAccept(boolean accept) {this.accept = accept;}
	public void addItem(ItemStack stack) {
		if(stack!=null && !stack.isEmpty()) {
			this.items.add(stack);
		}
	}
	public void addMessage(String msg) {
		if(msg!=null && !msg.isEmpty()) {
			this.msgs.add(msg);
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
		
		json.addProperty("title", this.title);
		json.addProperty("time", this.time);
		json.addProperty("sender", this.sender);
		if(read) json.addProperty("read", true);
		if(accept) json.addProperty("accept", true);
		if(this.hasSound()) json.add("sound", this.sound.toJson());
		if(this.hasItems()) {
			json.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		}
		if(this.hasMessages()) {
			JsonArray msgs = new JsonArray();
			for(int i = 0; i < this.msgs.size(); i++) {
				msgs.add(this.msgs.get(i));
			}
			json.add("msgs", msgs);
		}
		
		return json;
	}

	@Override
	public void read(JsonObject json) {
		if(json!=null && json.size()>0) {
			this.title = json.get("title").getAsString();
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
				JsonArray msgs = json.get("msgs").getAsJsonArray();
				for(int i = 0; i < msgs.size(); i++) {
					this.msgs.add(msgs.get(i).getAsString());
				}
			}
		}
	}

	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		
		nbt.setString("title", this.title);
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
			NBTTagList msgs = new NBTTagList();
			for(int i = 0; i < this.msgs.size(); i++) {
				msgs.appendTag(new NBTTagString(this.msgs.get(i)));
			}
			nbt.setTag("msgs", msgs);
		}
		
		return nbt;
	}

	@Override
	public void read(NBTTagCompound nbt) {
		if(nbt!=null && nbt.getSize()>0) {
			this.title = nbt.getString("title");
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
				NBTTagList msgs = nbt.getTagList("msgs", 8);
				for(int i = 0; i < msgs.tagCount(); i++) {
					this.msgs.add(msgs.getStringTagAt(i));
				}
			}
		}
	}
}
