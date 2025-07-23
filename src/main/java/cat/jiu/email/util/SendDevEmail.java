package cat.jiu.email.util;

import java.util.List;
import java.util.Map;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.element.EventEmail;
import cat.jiu.email.element.attachment.AttachmentAttribute;
import cat.jiu.email.element.attachment.AttachmentCommand;
import cat.jiu.email.element.attachment.AttachmentEffect;
import cat.jiu.email.element.attachment.AttachmentMaxHealth;
import cat.jiu.email.net.msg.MsgUnaccepted;
import cat.jiu.core.util.client.AudioSystem;
import com.google.common.collect.Lists;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendDevMessageEvent;
import cat.jiu.email.net.msg.MsgPlayerPermissionLevel;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class SendDevEmail {
	static final Email devEmail;
	static {
		List<IText> msgs = Lists.newArrayList();
		for(int i = 0; i < 14; i++) {
			msgs.add(new Text("inbox.dev_message."+i));
		}
		devEmail = new Email(new Text("inbox.dev_message.title"), EmailAPI.SYSTEM)
//				.setMcSound(new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundSource.PLAYERS))
				.addMessages(msgs)
				.addCommands(
						new AttachmentCommand.Cmd("/say this command is ' server ' command, use ' server console permission ' to execute.", true),
						new AttachmentCommand.Cmd("/me this command is ' player ' command, use ' player permission ' to execute.", false),
						new AttachmentCommand.Cmd("/say this command is hide in tooltip, you cant see this command.", true).setHideInTooltip(true),
						new AttachmentCommand.Cmd("/me this command is hide in tooltip, you cant see this command.", false).setHideInTooltip(true)
				)
				.setExperience(9980, 9980)
				.setExpirationTime(new TimeMillis(9999, 23, 59, 59, 9999))
				.setExternalSound(new AudioSystem.Audio("E:/application/tools/ffmpeg/bin/inbox_dev_sound.mp3", SoundSource.PLAYERS))
				.addAttachment(new AttachmentEffect()
						.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000))
						.addEffect(new MobEffectInstance(MobEffects.HEAL, 1000))
						.addEffect(new MobEffectInstance(MobEffects.JUMP, 1000))
						.addEffect(new MobEffectInstance(MobEffects.LUCK, 1000))
				)
				.addAttachment(new AttachmentMaxHealth(2))
				.addItem(new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 8))
				.addAttachment(new AttachmentAttribute()
						.addValue(Attributes.LUCK.value(), AttributeModifier.Operation.ADD_VALUE, new AttachmentAttribute.AttributeValue(20, false))
						.addValue(Attributes.ATTACK_DAMAGE.value(), AttributeModifier.Operation.ADD_VALUE, new AttachmentAttribute.AttributeValue(20, false))
						.addValue(Attributes.ATTACK_SPEED.value(), AttributeModifier.Operation.ADD_VALUE, new AttachmentAttribute.AttributeValue(20, false))
				)
				.setAccept(true);
	}
	
	public static Email getDevEmail() {
		return devEmail.setCreateTimeToNow();
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
			Inbox inbox = Inbox.get(player);
			
			int level = 0;
			ServerOpListEntry opEntry = player.getServer().getPlayerList().getOps().get(player.getGameProfile());
            if (opEntry != null) {
                level = opEntry.getLevel();
            }
			
			EmailMain.net.sendMessageToPlayer(new MsgPlayerPermissionLevel(level), player);
			
			if(!inbox.isSendDevMsg()) {
				inbox.setSendDevMsg(true);
				inbox.addEmail(getDevEmail().copy(), true);
				
//				EmailExecuteEvent.initDefaultCustomValue(inbox);
				NeoForge.EVENT_BUS.post(new EmailSendDevMessageEvent(player, inbox));
				EventEmail.sendEventEmails(EventEmail.Logged, player.getStringUUID());
			}
		}
	}
	
	@SubscribeEvent
	public static void onJoinWorld(EntityJoinLevelEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
			reminds.remove(player.getName().getString());
			Un un = Un.getInstance(player);
			if(un.unread > 0 || un.unReceived > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), player);
			}
		}
	}

	private static final Map<String, Delay> reminds = Maps.newHashMap();
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event){
		if(event.getEntity() instanceof ServerPlayer player){
			String name = player.getName().getString();
			if(!reminds.containsKey(name)) reminds.put(name, new Delay());
			Delay delay = reminds.get(name);
			if(delay.net >= 20){
				Un un = Un.getInstance(player);
				if(un.unread != delay.unread || un.unReceived != delay.unReceived) {
					delay.unread = un.unread;
					delay.unReceived = un.unReceived;
					EmailMain.net.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), player);
				}
				delay.net = 0;
			}else {
				delay.net++;
			}
		}
	}

	static int showToast;
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event){
		if (showToast >= EmailConfigClient.Prompt_Email.getTicks()) {
			if (EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
				Minecraft.getInstance().getToasts().addToast(new SystemToast(
						SystemToast.SystemToastId.PACK_COPY_FAILURE,
						Component.translatable("info.inbox.has_unread", EmailMain.getUnread()),
						Component.translatable("info.inbox.has_unreceive", EmailMain.getUnaccepted())
				));
			}
			showToast = 0;
		}else {
			showToast++;
		}
	}

	static class Delay {
		int net = 0;
		int unread = 0;
		int unReceived = 0;
	}
	record Un(int unread, int unReceived){
		static Un getInstance(Player player){
			Inbox inbox = Inbox.get(player);
			return new Un(inbox.getUnRead(), inbox.getUnReceived());
		}
	}
}
