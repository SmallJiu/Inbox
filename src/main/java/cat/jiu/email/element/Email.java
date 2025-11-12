package cat.jiu.email.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.util.*;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.core.util.element.sound.SoundJmp123;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.attachment.*;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TimeMillis;
import cat.jiu.sql.SQLValues;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public class Email implements IDataSerializable<IData.IMapData<?>> {
	public static final Email EMPTY = new Email(Text.empty, Text.empty).setRead(true).setReceive(true);

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

	protected ISound sound;

	public Email(@Nonnull IText title, @Nonnull IText sender) {
		this.title = title;
		this.sender = sender;
	}

	/**
	 * @param title 邮件标题
	 * @param sender 邮件发送者
	 * @param sound 邮件附带的音效
	 * @param items 邮件附带的物品
	 * @param messages 邮件附带的消息
	 *
	 * @author small_jiu
	 */
	public Email(@Nonnull IText title, @Nonnull IText sender, ISound sound, List<ItemStack> items, List<IText> messages) {
		this.title = title;
		this.sender = sender;
		this.sound = sound;
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
		this.setExternalSound(external_sound);
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
	}public Email(IData.IMapData<?> data) {
		this.read(data);
	}

	public boolean isEmpty(){
		return this == EMPTY
				|| (!this.hasMessages() && !this.hasAttachments());
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
	public ISound getSound() {return sound;}
	/**
	 * @return 邮件是否已读
	 */
	public boolean isRead() {return read;}
	/**
	 * @return 邮件是否已领
	 */
	public boolean isReceived() {return receive;}

	@Deprecated
	public boolean hasAttachments(ResourceLocation id) {
		return this.hasAttachment(id);
	}
	public boolean hasAttachment(ResourceLocation id) {
		return this.attachments!=null && this.attachments.containsKey(id);
	}

	/**
	 * 获取对应ID的附件
	 */
	public <T extends IAttachment> T getAttachment(ResourceLocation id) {
		if (!this.hasAttachment(id)) {
			this.addAttachment(IAttachment.REGISTRY.get(id, new JsonObject()));
		}
		return this.attachments.get(id).cast();
	}

	public Collection<IAttachment> getAttachments(){
		return this.hasAttachments() ? Collections.unmodifiableCollection(this.attachments.values()) : Collections.emptyList();
	}
	@Deprecated
	public boolean hasAttachment() {
		return this.hasAttachments();
	}
	public boolean hasAttachments() {
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
		if (attachment!=null){
			if (this.attachments == null) {
				this.attachments = new ConcurrentHashMap<>();
			}
			if (this.hasAttachment(attachment.getID())) {
				this.getAttachment(attachment.getID()).merge(attachment);
			} else {
				this.attachments.put(attachment.getID(), attachment);
			}
		}
		return this;
	}

	/**
	 * @return 邮件附带的物品
	 */
	public List<ItemStack> getItems() {
		if (this.hasAttachment(AttachmentItem.ID)) {
			return this.<AttachmentItem>getAttachment(AttachmentItem.ID).getItems();
		}
		return Collections.emptyList();
	}
	/**
	 * @return 邮件附带的效果
	 */
	public List<MobEffectInstance> getEffects() {
		if (this.hasAttachment(AttachmentEffect.ID)) {
			return this.<AttachmentEffect>getAttachment(AttachmentItem.ID).getEffects();
		}
		return Collections.emptyList();
	}
	/**
	 * @return 邮件附带的物品
	 */
	public List<AttachmentCommand.Cmd> getCommands() {
		if (this.hasAttachment(AttachmentCommand.ID)) {
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
			this.networkSize = EmailUtils.getSize((CompoundTag) this.write(NBTData.map()).getData());
		}
		return this.networkSize;
	}

	public String getFormatedNetworkSize(){
		return EmailUtils.getSize(this.getEmailNetworkSize());
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
		this.setSound(mc_sound);
	}
	public Email setSound(ISound sound) {
		this.sound = sound;
		return this;
	}

	/**
	 * @deprecated {@link Email#getExternalSound()}
	 */
	@Deprecated
	public AudioSystem.Audio getNetworkOrLocalSound(){
		return this.getExternalSound();
	}

	public AudioSystem.Audio getExternalSound(){
		return ((SoundJmp123)this.sound).getAudio();
	}

	/**
	 * @deprecated {@link Email#isExternalSound()}
	 */
	@Deprecated
	public boolean isNetworkOrLocalSound() {
		return this.isExternalSound();
	}
	public boolean isExternalSound() {
		return this.sound instanceof SoundJmp123;
	}

	/**
	 * NetworkOrLocalSound
	 * @deprecated {@link Email#setExternalSound(AudioSystem.Audio)}
	 * @param external_sound 本地或网络音效
	 */
	@Deprecated
	public Email setNetworkOrLocalSound(AudioSystem.Audio external_sound) {
		return this.setExternalSound(external_sound);
	}
	public Email setExternalSound(AudioSystem.Audio external_sound) {
		this.sound = new SoundJmp123(external_sound);
		return this;
	}

	/**
	 *
	 * @param externalSound 是否是本地或网络音效
	 */
	@Deprecated
	public Email setIsExternalSound(boolean externalSound) {
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
	public Email setCreateTime(long time) {
		this.create_time = time;
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
		this.expiration_time = expiration_time;
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
	 * @return 邮件是否对提供的时间戳过期
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
		if(this.hasAttachment(AttachmentItem.ID)) {
			return this.<AttachmentItem>getAttachment(AttachmentItem.ID).remove(slot);
		}
		return ItemStack.EMPTY;
	}
	/**
	 * 清空邮件附带的所有物品
	 */
	public Email clearItems() {
		if(this.hasAttachment(AttachmentItem.ID)) {
			this.<AttachmentItem>getAttachment(AttachmentItem.ID).removeAll();
		}
		return this;
	}
	/**
	 * 添加物品到邮件
	 */
	public Email addItems(List<ItemStack> stacks) {
		if(!this.hasAttachment(AttachmentItem.ID)) {
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
		if(!this.hasAttachment(AttachmentItem.ID)) {
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
		if(this.hasAttachment(AttachmentItem.ID)) {
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
		if(this.hasAttachment(AttachmentCommand.ID)) {
			return this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).removeCommand(slot);
		}
		return null;
	}
	/**
	 * 清空邮件附带的所有指令
	 */
	public Email clearAllCommands() {
		if(this.hasAttachment(AttachmentCommand.ID)) {
			this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).removeAllCommand();
		}
		return this;
	}
	/**
	 * 添加指令到邮件
	 */
	public Email addCommands(List<AttachmentCommand.Cmd> cmds) {
		if(!this.hasAttachment(AttachmentCommand.ID)) {
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
		if(!this.hasAttachment(AttachmentCommand.ID)) {
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
		if(this.hasAttachment(AttachmentCommand.ID)) {
			return this.<AttachmentCommand>getAttachment(AttachmentCommand.ID).setCommand(slot, newCmd);
		}
		return null;
	}

	public Email setExperience(int levels, int points) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().setLevels(levels).setPoints(points);
		return this;
	}
	public Email addExperienceLevel(int levels) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().addLevels(levels);
		return this;
	}
	public Email addExperiencePoint(int points) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		this.getAttachmentXP().addPoints(points);
		return this;
	}
	public Email subExperience(int levels, int points) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment
				.setPoints(attachment.getPoints() - points)
				.setLevels(attachment.getLevels() - levels);
		return this;
	}
	public Email subExperienceLevel(int levels) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment.setLevels(attachment.getLevels() - levels);
		return this;
	}
	public Email subExperiencePoint(int points) {
		if(!this.hasAttachment(AttachmentXP.ID)) {
			this.addAttachment(new AttachmentXP());
		}
		AttachmentXP attachment = this.getAttachmentXP();
		attachment.setPoints(attachment.getPoints() - points);
		return this;
	}
	public AttachmentXP getAttachmentXP(){
		if(!this.hasAttachment(AttachmentXP.ID)) {
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
			this.attachments.forEach((k,v) -> {
				try {
					v.accept(player);
				} catch (Exception e) {
					sendReceiveErrorMessage(player, v, e);
				}
			});
		}
	}

	public static void sendReceiveErrorMessage(Player player, IAttachment attachment, Exception e) {
		player.sendSystemMessage(Component.literal(ChatFormatting.RED + "[Inbox] Email receive error: "));
		player.sendSystemMessage(Component.literal(ChatFormatting.RED + "  - attachment: " + attachment.getID()));
		player.sendSystemMessage(Component.literal(ChatFormatting.RED + "  - error message: "));
		player.sendSystemMessage(Component.literal(ChatFormatting.RED + "    - " + e.getMessage()));
	}

	/**
	 * @return 是否带有附加的声音(音效)
	 */
	public boolean hasSound() {
		return this.sound !=null && this.sound.getAudioFile()!=null;
	}
	/**
	 * @return 是否带有附加的物品
	 */
	public boolean hasItems() {
		return this.hasAttachment(AttachmentItem.ID) && !this.getAttachment(AttachmentItem.ID).isEmpty();
	}
	public boolean hasCommands() {
		return this.hasAttachment(AttachmentCommand.ID) && !this.getAttachment(AttachmentCommand.ID).isEmpty();
	}
	public boolean hasXPs() {
		return this.hasAttachment(AttachmentXP.ID) && !this.getAttachment(AttachmentXP.ID).isEmpty();
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
			copy.sound = this.getSound().copy();
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
	public IData.IMapData<?> write(IData.IMapData<?> data) {
		data.putData("title", this.title.dynamicWrite(data));
		data.putData("sender", this.sender.dynamicWrite(data));
		data.putData("time", this.create_time);
		if(this.expiration_time!=null) {
			data.putData("expiration", this.expiration_time.millis);
		}

		if(this.isRead()) data.putData("read", true);
		if(this.isReceived()) data.putData("receive", true);
		data.putData("deletable", this.isDeletable());

		if(this.hasSound()) {
			data.putData("sound", this.sound.write(data.newMap()));
		}

		if (this.attachments!=null && !this.attachments.isEmpty()) {
			IData.IListData<?> attachments = data.newList();
			this.attachments.forEach((k,v)->{
				IData.IMapData<?> object = v.write(data.newMap());
				object.putData("id", String.valueOf(k));
				attachments.putData(object);
			});
			data.putData("attachments", attachments);
		}

		if(this.hasMessages()) {
			IData.IListData<?> msgs = data.newList();
			boolean anyType = msgs.getData() instanceof JsonArray;
			for (IText msg : this.messages) {
				if (anyType) {
					msgs.putData(msg.dynamicWrite(data));
				}else {
					msgs.putData(msg.write(data.newMap()));
				}
			}
			data.putData("msgs", msgs);
		}
		return data;
	}

	public static final SimpleDateFormat old_dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	@Override
	public void read(IData.IMapData<?> data) {
		this.title = IText.create(data.getData("title", data.newPrimitive().setData("")));
		this.sender = IText.create(data.getData("sender", data.newPrimitive().setData("")));

		if(data.containsKey("time")) {
			IData<?> time = data.getData("time", data.nullData());
			if (time.isPrimitive()) {
				IData.IPrimitiveData<?> p = time.getAsPrimitive();
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
		if(data.containsKey("expiration")) {
			this.expiration_time = new TimeMillis(data.getLong("expiration"));
			this.expiration_time_l = -404;
		}

		if(data.containsKey("read")) this.read = data.getBoolean("read");

		if(data.containsKey("accept")) {
			this.receive = data.getBoolean("accept");
		}else if(data.containsKey("receive")) {
			this.receive = data.getBoolean("receive");
		}
		this.setDeletable(data.getBoolean("deletable", true));

		if(data.containsKey("sound")) {
			if (data.getBoolean("network_local_sound", false)
					|| data.getBoolean("external_sound", false)
					|| data.getMap("sound").getBoolean("external_sound", false)) {
				IData.IMapData<?> external_sound = data.getMap("sound");

				if (external_sound.containsKey("category")) {
					external_sound.putData("channel", external_sound.getString("category"));
				}
				this.setExternalSound(AudioSystem.Audio.create(external_sound));
			}else {
				this.sound = ISound.REGISTRY.get(data.getMap("sound"));
			}
		}

		if(data.containsKey("items")) {
			this.addItems(DataUtils.toStack(data.getList("items", IData.IMapData.class)));
		}

		if (data.containsKey("attachments")) {
			data.getList("attachments", IData.IMapData.class).foreach((i,element)->{
				IData.IMapData<?> object = element.getAsMap();
				this.addAttachment(IAttachment.REGISTRY.get(object.getLocation("id"), object));
			});
		}

		if(data.containsKey("msgs")) {
			IData<?> msgElement = data.getData("msgs", data.nullData());
			if(msgElement.isMap()) {
				msgElement.getAsMap().foreach((k,v)->{
					if(v.isList()) {
						this.addMessage(new Text(k, IText.readArgs(v.getAsList())));
					}else if (v.isMap()){
						IText text = new Text(k);
						text.read(v.getAsMap());
						this.addMessage(text);
					}else if (v.isPrimitive()) {
						this.addMessage(new Text(v.getAsPrimitive().getAsString()));
					}else if (v.isNull()) {
						this.addMessage(Text.empty);
					}
				});
			}else if(msgElement.isList()) {
				msgElement.getAsList().foreach((k,v)->
					this.addMessage(IText.create(v))
				);
			}else if (msgElement.isPrimitive()) {
				this.addMessage(new Text(msgElement.getAsPrimitive().getAsString()));
			}else if (msgElement.isNull()) {
				this.addMessage(Text.empty);
			}
		}
	}

	public JsonObject write(JsonObject data) {
		this.write(JsonData.map(data));
		return data;
	}
	public void read(JsonObject data) {
		this.read(JsonData.map(data));
	}

	public CompoundTag write(CompoundTag data) {
		this.write(NBTData.map(data));
		return data;
	}
	public void read(CompoundTag data) {
		this.read(NBTData.map(data));
	}

	public SQLValues write(SQLValues value) {
		return value;
	}

	public void read(ResultSet result) throws SQLException {}

	@Override
	public String toString() {
		return this.write(JsonData.map()).asString();
	}

	@Override
	public Email clone() {
		return this.copy();
	}
}