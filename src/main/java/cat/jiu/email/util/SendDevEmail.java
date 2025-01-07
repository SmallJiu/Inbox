package cat.jiu.email.util;

import java.util.List;
import java.util.Map;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.attachment.AttachmentCommand;
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
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class SendDevEmail {
	static final Email devEmail;
	static {
		List<IText> msgs = Lists.newArrayList();
		for(int i = 0; i < 14; i++) {
			msgs.add(new Text("inbox.dev_message."+i));
		}
		devEmail = new Email(new Text("inbox.dev_message.title"), EmailAPI.SYSTEM)
//				.setMcSound(new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundCategory.PLAYERS))
				.addMessages(msgs)
				.addItem(new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 8))
				.addCommands(
						new AttachmentCommand.Cmd("/say 'this command is ' server ' command, use ' server console permission ' to execute.'", true),
						new AttachmentCommand.Cmd("/me 'this command is ' player ' command, use ' player permission ' to execute.'", false),
						new AttachmentCommand.Cmd("/say 'this command is hide in tooltip, you cant seed this command.'", true).setHideInTooltip(true),
						new AttachmentCommand.Cmd("/me 'this command is hide in tooltip, you cant seed this command.'", false).setHideInTooltip(true)
				)
				.setExperience(9980, 9980)
				.setExpirationTime(new TimeMillis(9999, 23, 59, 59, 9999))
				.setExternalSound(new AudioSystem.Audio("E:/application/tools/ffmpeg/bin/inbox_dev_sound.mp3", SoundCategory.PLAYERS))
				.setAccept(true);
	}
	
	public static Email getDevEmail() {
		return devEmail.setCreateTimeToNow();
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if(event.getPlayer() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
			Inbox inbox = Inbox.get(player);
			
			int level = 0;
			OpEntry opEntry = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());
            if (opEntry != null) {
                level = opEntry.getPermissionLevel();
            }
			
			EmailMain.net.sendMessageToPlayer(new MsgPlayerPermissionLevel(level), player);
			
			if(!inbox.isSendDevMsg()) {
				inbox.addEmail(devEmail.copy().setCreateTimeToNow());
				inbox.setSendDevMsg(true);
				
//				EmailExecuteEvent.initDefaultCustomValue(inbox);
				MinecraftForge.EVENT_BUS.post(new EmailSendDevMessageEvent(player, inbox));
				EmailUtils.saveInboxToDisk(inbox);
			}
		}
	}
	
	@SubscribeEvent
	public static void onJoinWorld(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof ServerPlayerEntity) {
			Un un = Un.getInstance((PlayerEntity) event.getEntity());
			if(un.unread > 0 || un.unReceived > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), (ServerPlayerEntity) event.getEntity());
			}
		}
	}

	private static final Map<String, Delay> reminds = Maps.newHashMap();
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event){
		if(event.player instanceof ServerPlayerEntity && event.phase == TickEvent.Phase.END){
			String name = event.player.getName().getString();
			if(!reminds.containsKey(name)) reminds.put(name, new Delay());
			Delay delay = reminds.get(name);
			if(delay.net >= 10){
				Un un = Un.getInstance(event.player);
				if(un.unread != delay.unread || un.unReceived != delay.unReceived) {
					delay.unread = un.unread;
					delay.unReceived = un.unReceived;
					EmailMain.net.sendMessageToPlayer(new MsgUnaccepted(un.unread, un.unReceived), (ServerPlayerEntity) event.player);
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
	public static void onClientTick(TickEvent.WorldTickEvent event){
		if (event.phase == TickEvent.Phase.END) {
			if (EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
				long s = showToast - System.currentTimeMillis();
				if (s <= 0) {
					Minecraft.getInstance().getToastGui().add(new SystemToast(
							SystemToast.Type.PACK_COPY_FAILURE,
							new TranslationTextComponent("info.inbox.has_unread", EmailMain.getUnread()),
							new TranslationTextComponent("info.inbox.has_unreceive", EmailMain.getUnaccepted())
					));
					showToast = System.currentTimeMillis() + 10 * 1000;
				}
			}
		}
	}

	static class Delay {
		int net = 0;
		int unread = 0;
		int unReceived = 0;
	}

	static class Un{
		public final int unread, unReceived;

		public Un(int unread, int unReceived) {
			this.unread = unread;
			this.unReceived = unReceived;
		}

		static Un getInstance(PlayerEntity player){
			Inbox inbox = Inbox.get(player);
			return new Un(inbox.getUnRead(), inbox.getUnReceived());
		}
	}
}
