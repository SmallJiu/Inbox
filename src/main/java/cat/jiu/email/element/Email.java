package cat.jiu.email.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.attachment.*;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.api.serializable.ISerializable;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.core.util.JsonToStackUtil;
import cat.jiu.email.util.TimeMillis;
import cat.jiu.sql.SQLValues;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public class Email implements ISerializable {
	protected IText title;
	protected IText sender;

	protected long create_time = System.currentTimeMillis();
	protected String create_time_s;

	protected boolean read;
	protected boolean receive;
	protected boolean deletable = true;

	protected TimeMillis expiration_time;
	protected long expiration_time_l;

	protected List<IText> messages;

	protected Map<ResourceLocation, IAttachment> attachments;

	protected ISound mc_sound;
	protected AudioSystem.Audio external_sound;
	protected boolean isExternalSound;

	public Email(@Nonnull IText title, @Nonnull IText sender) {
		this.title = title;
		this.sender = sender;
	}

	/**
	 * @param title 邮件标题
	 * @param sender 邮件发送者
	 * @param mc_sound 邮件附带的音效
	 * @param items 邮件附带的物品
	 * @param messages 邮件附带的消息
	 *
	 * @author small_jiu
	 */
	public Email(@Nonnull IText title, @Nonnull IText sender, ISound mc_sound, List<ItemStack> items, List<IText> messages) {
		this.title = title;
		this.sender = sender;
		this.mc_sound = mc_sound;
		if (items!=null && !items.isEmpty()) {
			this.addItems(items);
		}
		this.messages = messages;
	}

	/**
	 * @param title 邮件标题
	 * @param sender 邮件发送者
	 * @param external_sound 邮件附带的音效
	 * @param items 邮件附带的物品
	 * @param messages 邮件附带的消息
	 *
	 * @author small_jiu
	 */
	public Email(@Nonnull IText title, @Nonnull IText sender, AudioSystem.Audio external_sound, List<ItemStack> items, List<IText> messages) {
		this.title = title;
		this.sender = sender;
		this.external_sound = external_sound;
		this.isExternalSound = true;
		if (items!=null && !items.isEmpty()) {
			this.addItems(items);
		}
		this.messages = messages;
	}

	public Email(CompoundTag nbt) {
		this.read(nbt);
	}
	public Email(JsonObject json) {
		this.read(json);
	}

	public boolean isDeletable() {
		return deletable;
	}

	public Email setDeletable(boolean deletable) {
		this.deletable = deletable;
		return this;
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
	@Nonnull
	public String getCreateTimeAsString() {
		if(this.create_time_s == null) {
			this.create_time_s = this.getCreateTimeAsString(EmailUtils.dateFormat);
		}
		return this.create_time_s;
	}
	@Nonnull
	public String getCreateTimeAsString(SimpleDateFormat format) {
		return format.format(new Date(this.create_time));
	}
	/**
	 * @return 邮件标题
	 */
	@Nonnull
	public IText getTitle() {return title;}
	/**
	 * @return 邮件发送者
	 */
	@Nonnull
	public IText getSender() {return sender;}
	/**
	 * @return 邮件附带的声音(音效)
	 */
	@Nullable
	public ISound getSound() {return mc_sound;}
	/**
	 * @return 邮件是否已读
	 */
	public boolean isRead() {return read;}
	/**
	 * @return 邮件是否已领
	 */
	public boolean isReceived() {return receive;}

	public boolean hasAttachments(ResourceLocation id) {
		return this.attachments!=null && this.attachments.containsKey(id);
	}

	/**
	 * 获取对应ID的附件
	 */
	public <T extends IAttachment> T getAttachment(ResourceLocation id) {
		if (!this.hasAttachments(id)) {
			this.addAttachment(IAttachment.REGISTRY.get(id, new JsonObject()));
		}
		return this.attachments.get(id).cast();
	}

	public Collection<IAttachment> getAttachments(){
		return this.hasAttachment() ? Collections.unmodifiableCollection(this.attachments.values()) : Collections.emptyList();
	}
	public boolean hasAttachment() {
		if (this.attachments!=null) {
			for (IAttachment value : this.attachments.values()) {
				if (!value.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 添加一种附件，已有同一种附件的情况下，会合并到已有附件内
	 */
	public Email addAttachment(IAttachment attachment) {
		if (this.attachments==null) {
			this.attachments = new ConcurrentHashMap<>();
		}
		if (this.hasAttachments(attachment.getID())) {
			this.getAttachment(attachment.getID()).merge(attachment);
		}else {
			this.attachments.put(attachment.getID(), attachment);
		}
		return this;
	}

	/**
	 * @return 邮件附带的物品
	 */
	public List<ItemStack> getItems() {
		if (this.hasAttachments(AttachmentItem.ID)) {
			return this.<AttachmentItem>getAttachment(AttachmentItem.ID).getItems();
		}
		return Collections.emptyList();
	}
	/**
	 * @return 邮件附带的效果
	 */
	public List<MobEffectInstance> getEffects() {
		if (this.hasAttachments(AttachmentEffect.ID)) {
			return this.<AttachmentEffect>getAttachment(AttachmentItem.ID).getEffects();
		}
		return Collections.emptyList();
	}
	/**
	 * @return 邮件附带的物品
	 */
	public List<AttachmentCommand.Cmd> getCommands() {
		if (this.hasAttachments(AttachmentCommand.ID)) {
			return this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).getCommands();
		}
		return Collections.emptyList();
	}
	/**
	 * @return 邮件附带的消息
	 */
	public List<IText> getMessages() {
		return messages==null?Collections.emptyList() : messages;
	}

	protected long networkSize = -404;
	/**
	 * @return 邮件的网络包大小
	 */
	public long getEmailNetworkSize() {
		if(this.networkSize == -404) {
			this.networkSize = EmailUtils.getSize(this.writeTo(CompoundTag.class));
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
	 * @param receive 邮件是否已领
	 */
	public Email setReceive(boolean receive) {this.receive = receive; return this;}
	@Deprecated
	public Email setAccept(boolean receive) {return this.setReceive(receive);}

	public void setMcSound(ISound mc_sound) {
		this.mc_sound = mc_sound;
	}

	@Deprecated
	public AudioSystem.Audio getNetworkOrLocalSound(){
		return this.getExternalSound();
	}

	public AudioSystem.Audio getExternalSound(){
		return external_sound;
	}

	@Deprecated
	public boolean isNetworkOrLocalSound() {
		return this.isExternalSound();
	}
	public boolean isExternalSound() {
		return isExternalSound;
	}

	/**
	 * NetworkOrLocalSound
	 * @param external_sound 本地或网络音效
	 */
	@Deprecated
	public Email setNetworkOrLocalSound(AudioSystem.Audio external_sound) {
		return this.setExternalSound(external_sound);
	}
	public Email setExternalSound(AudioSystem.Audio external_sound) {
		this.external_sound = external_sound;
		this.setIsExternalSound(true);
		return this;
	}

	/**
	 *
	 * @param externalSound 是否是本地或网络音效
	 */
	public Email setIsExternalSound(boolean externalSound) {
		isExternalSound = externalSound;
		return this;
	}
	@Deprecated
	public Email setIsNetworkOrLocalSound(boolean externalSound) {
		return this.setIsExternalSound(externalSound);
	}

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
	 */
	public Email setMessageParameters(int msgIndex, int index, Object obj) {
		this.getMessages().get(msgIndex).getParameters()[index] = obj;
		return this;
	}

	/**
	 * 设置邮件发送者的附加内容
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
		if(this.hasAttachments(AttachmentItem.ID)) {
			return this.<AttachmentItem>getAttachment(AttachmentItem.ID).remove(slot);
		}
		return ItemStack.EMPTY;
	}
	/**
	 * 清空邮件附带的所有物品
	 */
	public Email clearItems() {
		if(this.hasAttachments(AttachmentItem.ID)) {
			this.<AttachmentItem>getAttachment(AttachmentItem.ID).removeAll();
		}
		return this;
	}
	/**
	 * 添加物品到邮件
	 */
	public Email addItems(List<ItemStack> stacks) {
		if(!this.hasAttachments(AttachmentItem.ID)) {
			this.addAttachment(new AttachmentItem());
		}
		if (stacks!=null && !stacks.isEmpty()){
			this.<AttachmentItem>getAttachment(AttachmentItem.ID).addStacks(stacks);
		}
		return this;
	}
	/**
	 * 添加物品到邮件
	 */
	public Email addItem(ItemStack... stacks) {
		if(!this.hasAttachments(AttachmentItem.ID)) {
			this.addAttachment(new AttachmentItem());
		}
		if (stacks!=null && stacks.length > 0) {
			this.<AttachmentItem>getAttachment(AttachmentItem.ID).addStack(stacks);
		}
		return this;
	}
	/**
	 * 设置邮件的附加指令
	 */
	public ItemStack setItem(int slot, ItemStack newItem) {
		if(this.hasAttachments(AttachmentItem.ID)) {
			return this.<AttachmentItem>getAttachment(AttachmentItem.ID).setStack(slot, newItem);
		}
		return ItemStack.EMPTY;
	}

	/**
	 * 移除附加指令
	 * @param slot slot
	 * @return 已被移除指令
	 */
	public AttachmentCommand.Cmd removeCommand(int slot) {
		if(this.hasAttachments(AttachmentCommand.ID)) {
			return this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).removeCommand(slot);
		}
		return null;
	}
	/**
	 * 清空邮件附带的所有指令
	 */
	public Email clearAllCommands() {
		if(this.hasAttachments(AttachmentCommand.ID)) {
			this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).removeAllCommand();
		}
		return this;
	}
	/**
	 * 添加指令到邮件
	 */
	public Email addCommands(List<AttachmentCommand.Cmd> cmds) {
		if(!this.hasAttachments(AttachmentCommand.ID)) {
			this.addAttachment(new AttachmentCommand());
		}
		if (cmds!=null){
			cmds.forEach(this.<AttachmentCommand>getAttachment(AttachmentCommand.ID)::addCommand);
		}
		return this;
	}
	/**
	 * 添加指令到邮件
	 */
	public Email addCommands(AttachmentCommand.Cmd... cmds) {
		if(!this.hasAttachments(AttachmentCommand.ID)) {
			this.addAttachment(new AttachmentCommand());
		}
		if (cmds!=null){
			for (AttachmentCommand.Cmd cmd : cmds) {
				this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).addCommand(cmd);
			}
		}
		return this;
	}
	/**
	 * 设置邮件的附加指令
	 */
	public AttachmentCommand.Cmd setCommand(int slot, AttachmentCommand.Cmd newCmd) {
		if(this.hasAttachments(AttachmentCommand.ID)) {
			return this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).setCommand(slot, newCmd);
		}
		return null;
	}

	public Email setExperience(int levels, int points) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().setLevels(levels).setPoints(points);
		return this;
	}
	public Email addExperienceLevel(int levels) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().addLevels(levels);
		return this;
	}
	public Email addExperiencePoint(int points) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().addPoints(points);
		return this;
	}
	public Email subExperience(int levels, int points) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment
				.setPoints(attachment.getPoints() - points)
				.setLevels(attachment.getLevels() - levels);
		return this;
	}
	public Email subExperienceLevel(int levels) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment.setLevels(attachment.getLevels() - levels);
		return this;
	}
	public Email subExperiencePoint(int points) {
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment.setPoints(attachment.getPoints() - points);
		return this;
	}
	public AttachmentXP getAttachmentXP(){
		if(!this.hasAttachments(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		return this.getAttachment(AttachmentXP.ID);
	}

	/**
	 * 添加新消息到邮件
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
	 */
	public Email addMessage(IText text) {
		if(this.messages ==null) this.messages =Lists.newArrayList();
		if(text!=null) this.messages.add(text);
		return this;
	}
	public Email addMessages(Collection<IText> text) {
		for (IText msg : text) {
			this.addMessage(msg);
		}
		return this;
	}
	public Email addMessages(IText... text) {
		for (IText msg : text) {
			this.addMessage(msg);
		}
		return this;
	}
	/**
	 * 设置邮件的附加消息
	 */
	public Email setMessage(int index, String msg, Object... args) {
		if(msg!=null && !msg.isEmpty()) {
			this.setMessage(index, new Text(msg, args));
		}
		return this;
	}
	/**
	 * 设置邮件的附加消息
	 */
	public Email setMessage(int index, IText text) {
		if(this.messages ==null || this.messages.isEmpty()) return this;
		if(index >= 0 && index < this.messages.size()) {
			this.messages.set(index, text);
		}
		return this;
	}
	/**
	 * 移除邮件的附加消息
	 */
	public IText removeMessage(int index) {
		if(this.messages ==null || this.messages.isEmpty()) return null;
		if(index >= 0 && index < this.messages.size()) {
			return this.messages.remove(index);
		}
		return null;
	}

	public void receive(Player player) {
		if (!this.isReceived() && this.attachments!=null) {
			this.setReceive(true);
			this.attachments.forEach((k,v) -> v.accept(player));
		}
	}

	/**
	 * @return 是否带有附加的声音(音效)
	 */
	public boolean hasSound() {
		return (this.mc_sound !=null && this.mc_sound.getAudioFile()!=null) || (this.external_sound != null && !this.external_sound.getFile().isEmpty());
	}
	/**
	 * @return 是否带有附加的物品
	 */
	public boolean hasItems() {
		return this.hasAttachments(AttachmentItem.ID) && !this.getAttachment(AttachmentItem.ID).isEmpty();
	}
	public boolean hasCommands() {
		return this.hasAttachments(AttachmentCommand.ID) && !this.getAttachment(AttachmentCommand.ID).isEmpty();
	}
	public boolean hasXPs() {
		return this.hasAttachments(AttachmentXP.ID) && !this.getAttachment(AttachmentXP.ID).isEmpty();
	}
	/**
	 * @return 是否带有附加的消息
	 */
	public boolean hasMessages() {
		return this.messages !=null && !this.messages.isEmpty();
	}

	/**
	 * @return 新的邮件对象
	 */
	public Email copy() {
		Email copy = new Email(this.getTitle().copy(), this.getSender().copy());
		if(this.hasSound()) {
			if (this.isExternalSound()) {
				copy.external_sound = this.external_sound;
			}else {
				copy.mc_sound = this.getSound().copy();
			}
		}
		if(this.attachments!=null) {
			this.attachments.forEach((k,v)->copy.addAttachment(v.copy()));
		}
		if(this.hasMessages()){
			List<IText> msgs = Lists.newArrayList();
			for(int i = 0; i < this.getMessages().size(); i++) {
				msgs.add(this.getMessages().get(i).copy());
			}
			copy.messages = msgs;
		}
		if (this.hasExpirationTime()) {
			copy.setExpirationTime(this.getExpirationTime());
		}
		copy.setRead(this.isRead());
		copy.setReceive(this.isReceived());

		return copy;
	}

	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();

		json.add("title", this.title.write(new JsonObject()));
		json.addProperty("time", this.create_time);
		if(this.expiration_time!=null) {
			json.addProperty("expiration", this.expiration_time.millis);
		}

		json.add("sender", this.sender.write(new JsonObject()));

		if(this.isRead()) json.addProperty("read", true);
		if(this.isReceived()) json.addProperty("receive", true);
		json.addProperty("deletable", this.isDeletable());

		if(this.hasSound()) {
			if (this.isExternalSound()) {
				JsonObject sound_obj = new JsonObject();
				sound_obj.addProperty("file", this.getExternalSound().getFile());
				sound_obj.addProperty("category", this.getExternalSound().getCategory().getName());
				json.add("sound", sound_obj);
				json.addProperty("external_sound", true);
			}else if(this.mc_sound != null) {
				json.add("sound", this.mc_sound.write(new JsonObject()));
			}
		}

		if (this.attachments!=null && !this.attachments.isEmpty()) {
			JsonArray attachments = new JsonArray();
			this.attachments.forEach((k,v)->{
				JsonObject object = v.write(new JsonObject());
				object.addProperty("id", String.valueOf(k));
				attachments.add(object);
			});
			json.add("attachments", attachments);
		}

		if(this.hasMessages()) {
			JsonObject msgs = new JsonObject();
			for (IText msg : this.messages) {
				if (msg.getParameters() != null && msg.getParameters().length > 0) {
					msgs.add(msg.getText(), msg.writeArgs(new JsonArray()));
				} else {
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
			if(json.has("title") && json.get("title").isJsonObject()) {
				this.title = new Text(json.getAsJsonObject("title"));
			}else if(json.has("title") && json.get("title").isJsonPrimitive()) {
				this.title = new Text(json.get("title").getAsString());
			}

			if(json.has("sender") && json.get("sender").isJsonObject()) {
				this.sender = new Text(json.getAsJsonObject("sender"));
			}else if(json.has("sender") && json.get("sender").isJsonPrimitive()) {
				this.sender = new Text(json.get("sender").getAsString());
			}

			if(json.has("time")) {
				JsonElement time = json.get("time");
				if (time.isJsonPrimitive()) {
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
			}
			if(json.has("expiration")) {
				this.expiration_time = new TimeMillis(json.get("expiration").getAsLong());
				this.expiration_time_l = -404;
			}

			if(json.has("read")) this.read = json.get("read").getAsBoolean();

			if(json.has("accept")) {
				this.receive = json.get("accept").getAsBoolean();
			}else if(json.has("receive")) {
				this.receive = json.get("receive").getAsBoolean();
			}
			this.setDeletable(JsonUtils.get(json, "deletable", true));

			if(json.has("sound")) {
				if (JsonUtils.get(json, "network_local_sound", false)
				 || JsonUtils.get(json, "external_sound", false)) {
					JsonObject network_local = json.getAsJsonObject("sound");
					this
							.setExternalSound(new AudioSystem.Audio(
									network_local.get("file").getAsString(),
									network_local.has("category") ?
											EmailUtils.getSoundSource(network_local.get("category").getAsString())
											: SoundSource.PLAYERS
							));
				}else {
					this.mc_sound = ISound.REGISTRY.get(SoundMC.ID, json.getAsJsonObject("sound"));
				}
			}

			if(json.has("items")) {
				this.addItems(JsonToStackUtil.toStacks(json.get("items")));
			}

			if (json.has("attachments")) {
				for (JsonElement element : json.getAsJsonArray("attachments")) {
					JsonObject object = element.getAsJsonObject();
					this.addAttachment(IAttachment.REGISTRY.get(Utils.location(object.get("id").getAsString()), object));
				}
			}

			if(json.has("msgs")) {
				this.messages = Lists.newArrayList();
				JsonElement msgElement = json.get("msgs");
				if(msgElement.isJsonObject()) {
					JsonObject msgs = msgElement.getAsJsonObject();
					for(Entry<String, JsonElement> msg : msgs.entrySet()) {
						if(msg.getValue().isJsonArray()) {
							this.messages.add(new Text(msg.getKey(), IText.readArgs(msg.getValue().getAsJsonArray())));
						}else {
							this.messages.add(new Text(msg.getKey()));
						}
					}
				}else if(msgElement.isJsonArray()) {
					JsonArray msgs = msgElement.getAsJsonArray();
					for(int i = 0; i < msgs.size(); i++) {
						JsonElement msg = msgs.get(i);
						if(msg.isJsonObject()) {
							JsonObject object = msg.getAsJsonObject();
							this.messages.add(new Text(object.get("text").getAsString(), IText.readArgs(object.getAsJsonArray("parameters"))));
						} else if (msg.isJsonNull()) {
							this.messages.add(new Text(msg.getAsString()));
						}
					}
				}
			}
		}
	}

	protected static Tag emptyTag = null;
	public static Tag getEmptyTag() {
		if(emptyTag==null) {
			emptyTag = ByteTag.valueOf((byte)0);
		}
		return emptyTag;
	}

	@Override
	public CompoundTag write(CompoundTag nbt) {
		if(nbt==null) nbt = new CompoundTag();

		nbt.put("title", this.title.writeTo(CompoundTag.class));
		nbt.putLong("time", this.create_time);
		if(this.expiration_time!=null) {
			nbt.putLong("expiration", this.expiration_time.millis);
		}
		nbt.put("sender", this.sender.writeTo(CompoundTag.class));
		if(this.isRead()) nbt.putBoolean("read", this.isRead());
		if(this.isReceived()) nbt.putBoolean("receive", this.isReceived());
		if (!this.isDeletable()) nbt.putBoolean("deletable", this.isDeletable());
		if(this.hasSound()) {
			if (this.isExternalSound()) {
				CompoundTag sound_obj = new CompoundTag();
				sound_obj.putString("file", this.getExternalSound().getFile());
				sound_obj.putString("category", this.getExternalSound().getCategory().getName());
				nbt.put("sound", sound_obj);
				nbt.putBoolean("external_sound", true);
			}else if(this.mc_sound != null) {
				nbt.put("sound", this.mc_sound.write(new CompoundTag()));
			}
		}

		if (this.attachments!=null && !this.attachments.isEmpty()) {
			ListTag attachments = new ListTag();
			this.attachments.forEach((k,v)->{
				CompoundTag object = v.writeTo(CompoundTag.class);
				object.putString("id", String.valueOf(k));
				attachments.add(object);
			});
			nbt.put("attachments", attachments);
		}

		if(this.hasMessages()) {
			CompoundTag msgs = new CompoundTag();
			for(int i = 0; i < this.messages.size(); i++) {
				IText msg = this.messages.get(i);
				Tag msgNBT;
				if(msg.getParameters()!=null && msg.getParameters().length>0) {
					msgNBT = msg.write(new CompoundTag());
				}else if(!"".equals(msg.getText())) {
					msgNBT = StringTag.valueOf(msg.getText());
				}else {
					msgNBT = getEmptyTag();
				}
				msgs.put(String.valueOf(i), msgNBT);
			}
			nbt.put("msgs", msgs);
		}

		return nbt;
	}

	@Override
	public void read(CompoundTag nbt) {
		if(nbt!=null && !nbt.isEmpty()) {
			this.title = new Text(nbt.getCompound("title"));
			this.create_time = nbt.getLong("time");
			if(nbt.contains("expiration")) {
				this.expiration_time = new TimeMillis(nbt.getLong("expiration"));
				this.expiration_time_l = -404;
			}
			this.sender = new Text(nbt.getCompound("sender"));
			if(nbt.contains("read")) this.read = nbt.getBoolean("read");
			if(nbt.contains("receive")) this.receive = nbt.getBoolean("receive");
			this.setDeletable(NBTUtils.get(nbt, "deletable", true));
			if(nbt.contains("sound")) {
				if (nbt.contains("external_sound") && nbt.getBoolean("external_sound")) {
					CompoundTag network_local = nbt.getCompound("sound");
					this
							.setExternalSound(new AudioSystem.Audio(
									network_local.getString("file"),
									network_local.contains("category") ?
											EmailUtils.getSoundSource(network_local.getString("category"))
											: SoundSource.PLAYERS
							));
				}else {
					this.mc_sound = ISound.REGISTRY.get(SoundMC.ID, nbt.getCompound("sound"));
				}
			}

			if(nbt.contains("items")) {
				CompoundTag items = nbt.getCompound("items");
				for(String item : items.getAllKeys()) {
					this.addItem(ItemStack.of(items.getCompound(item)));
				}
			}

			if (nbt.contains("attachments")) {
				ListTag attachments = nbt.getList("attachments", 10);
				for (int i = 0; i < attachments.size(); i++) {
					CompoundTag object = attachments.getCompound(i);
					this.addAttachment(IAttachment.REGISTRY.get(Utils.location(object.get("id").getAsString()), object));
				}
			}

			if(nbt.contains("msgs")) {
				this.messages = Lists.newArrayList();
				CompoundTag msgs = nbt.getCompound("msgs");
				List<String> keys = msgs.getAllKeys().stream().sorted(Comparator.comparing(Integer::valueOf)).collect(Collectors.toList());
				keys.forEach(key->{
					Tag msg = msgs.get(key);
					if(msg instanceof StringTag) {
						this.messages.add(new Text(msg.getAsString()));
					}else if(msg instanceof CompoundTag text) {
						this.messages.add(new Text(text.getString("text"), IText.readArgs(text.getList("args", 8))));
					}else if(msg instanceof ByteTag) {
						this.messages.add(Text.empty);
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
}