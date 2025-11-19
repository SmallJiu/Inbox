package cat.jiu.email.util;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.command.EmailFileType;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.EventEmail;
import cat.jiu.email.element.attachment.*;
import cat.jiu.email.net.msg.MsgEmailFiles;
import cat.jiu.email.net.msg.MsgSendCooling;
import cat.jiu.email.net.msg.MsgUnaccepted;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.email.ui.gui.GuiInbox;
import com.google.common.collect.Lists;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendDevMessageEvent;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SendDevEmail {
	static final Email devEmail;
	static {
		File default_sound = new File("./assets/jiu/inbox", "default_sound.mp3");
		List<IText> msgs = Lists.newArrayList();
		for(int i = 0; i < 14; i++) {
			msgs.add(new Text("inbox.dev_message."+i));
		}
		msgs.add(new Text("%s of %s of %s of %s",
				new Text("inbox.dev_message.title"),
				Component.translatable("info.inbox.main.from", "981314"),
				Component.literal("9856168549616596416548"),
				"99999"
		));
		devEmail = new Email(new Text("inbox.dev_message.title"), EmailAPI.SYSTEM)
//				.setMcSound(new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundSource.PLAYERS))
				.setExpirationTime(new TimeMillis(9999, 23, 59, 59, 9999))
				.addMessages(msgs)
				.setExternalSound(new AudioSystem.Audio(default_sound, default_sound.exists() ? null : EmailMain.class.getResourceAsStream("/assets/email/sounds/default.mp3"), SoundSource.PLAYERS).addRetryFile("https://raw.githubusercontent.com/SmallJiu/Document/refs/heads/master/mods/Inbox/default.mp3")
						.setCanLopping(true)
						.setLoopDelay(500)
				)
				.addItem(
						new ItemStack(Items.DIAMOND, 9),
						new ItemStack(Items.DIAMOND, 9),
						new ItemStack(Items.DIAMOND, 8)
				)
				.setExperience(9980, 9980)
				.addAttachment(new AttachmentMaxHealth(2))
				.addAttachment(new AttachmentUndying(9))
				.addCommands(
						new AttachmentCommand.Cmd("/say this command is ' server ' command, use ' server console permission ' to execute.", true),
						new AttachmentCommand.Cmd("/me this command is ' player ' command, use ' player permission ' to execute.", false),
						new AttachmentCommand.Cmd("/say this command is hide in tooltip, you cant see this command.", true).setHideInTooltip(true),
						new AttachmentCommand.Cmd("/me this command is hide in tooltip, you cant see this command.", false).setHideInTooltip(true)
				)
				.addAttachment(new AttachmentEffect()
						.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000))
						.addEffect(new MobEffectInstance(MobEffects.HEAL, 1000))
						.addEffect(new MobEffectInstance(MobEffects.JUMP, 1000))
						.addEffect(new MobEffectInstance(MobEffects.LUCK, 1000))
				)
				.addAttachment(new AttachmentAttribute()
						.addValue(Attributes.LUCK, AttributeModifier.Operation.ADDITION, new AttachmentAttribute.AttributeValue(20, false))
						.addValue(Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.MULTIPLY_BASE, new AttachmentAttribute.AttributeValue(20, false))
						.addValue(Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, new AttachmentAttribute.AttributeValue(20, false))
				)
				.addAttachment(new AttachmentWaypoint()
						.addWaypoint(Level.OVERWORLD, BlockPos.ZERO, "Overworld Zero Point", Color.WHITE.getRGB())
						.addWaypoint(Level.NETHER, BlockPos.ZERO, "Nether Zero Point", Color.WHITE.getRGB())
						.addWaypoint(Level.END, BlockPos.ZERO, "End Zero Point", Color.WHITE.getRGB())
				)
				.setReceive(true);
	}
	
	public static Email getDevEmail() {
		return devEmail.setCreateTimeToNow();
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
//			EmailMain.NETWORK.sendMessageToPlayer(new MsgEmailFiles(EmailFileType.getFiles()), player);
			Inbox inbox = Inbox.get(player);

			player.getServer().getPlayerList().sendPlayerPermissionLevel(player);
			EmailMain.NETWORK.sendMessageToPlayer(new MsgSendCooling(inbox.getSendCooling()), player);
			
			if(!inbox.isSendDevMsg()) {
				inbox.setSendDevMsg(true);
				if (EmailConfigServer.Enable_New_Player_Email.get()) {
					inbox.addEmail(getDevEmail().copy());
				}
				EmailUtils.saveInboxToDisk(inbox);

				MinecraftForge.EVENT_BUS.post(new EmailSendDevMessageEvent(player, inbox));
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
				EmailMain.NETWORK.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), player);
			}
		}
	}

	private static final Map<String, Delay> reminds = Maps.newHashMap();
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event){
		if(event.player instanceof ServerPlayer player && event.phase == TickEvent.Phase.END){
			String name = player.getName().getString();
			if(!reminds.containsKey(name)) reminds.put(name, new Delay());
			Delay delay = reminds.get(name);
			if(delay.net >= 20){
				Un un = Un.getInstance(player);
				if(un.unread != delay.unread || un.unReceived != delay.unReceived) {
					delay.unread = un.unread;
					delay.unReceived = un.unReceived;
					EmailMain.NETWORK.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), player);
				}
				delay.net = 0;
			}else {
				delay.net++;
			}
		}
	}

	static long showToast;
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event){
		if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
			if (System.currentTimeMillis() >= showToast) {
				int unread = GuiInbox.INBOX.getUnRead(),
						unReceived = GuiInbox.INBOX.getUnReceived();
				if (unread > 0 || unReceived > 0) {
					Component
							title = CommonComponents.EMPTY,
							message = CommonComponents.EMPTY;
					if (unread > 0) {
						title = Component.translatable("info.inbox.has_unread", EmailMain.getUnread());
					}
					if (unReceived > 0) {
						message = Component.translatable("info.inbox.has_unreceive", EmailMain.getUnaccepted());
					}

					Minecraft.getInstance().getToasts().addToast(new SystemToast(
							SystemToast.SystemToastIds.PACK_COPY_FAILURE, title, message
					));
				}
				showToast = System.currentTimeMillis() + EmailConfigClient.Prompt_Email.getMillis();
			}
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
