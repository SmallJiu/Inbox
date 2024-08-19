package cat.jiu.email.util;

import java.util.List;
import java.util.Map;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.element.attachment.AttachmentCommand;
import cat.jiu.email.net.msg.MsgUnreceived;
import cat.jiu.formless.utils.client.AudioSystem;
import com.google.common.collect.Lists;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendDevMessageEvent;
import cat.jiu.email.net.msg.MsgPlayerPermissionLevel;
import cat.jiu.email.net.msg.MsgUnread;

import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class SendDevEmail {
	static final Email devEmail;
	static {
		List<IText> msgs = Lists.newArrayList();
		for(int i = 0; i < 14; i++) {
			msgs.add(new Text("email.dev_message."+i));
		}
		devEmail = new Email(new Text("email.dev_message.title"), EmailAPI.SYSTEM)
//				.setMcSound(new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundSource.PLAYERS))
				.addMessages(msgs)
				.addItem(new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 8))
				.addCommands(
						new AttachmentCommand.Cmd("/say this command is ' server ' command, user ' server console permission ' to execute.", true),
						new AttachmentCommand.Cmd("/me this command is ' player ' command, user ' player permission ' to execute.", false),
						new AttachmentCommand.Cmd("/me this command is hide in tooltip, you cant seed this command.", false).setHideInTooltip(true)
				)
				.setExperience(9980, 9980)
				.setExpirationTime(new TimeMillis(9999, 23, 59, 59, 9999))
				.setExternalSound(new AudioSystem.Audio("E:/application/tools/ffmpeg/bin/inbox_dev_sound.mp3", SoundSource.PLAYERS))
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
				inbox.addEmail(devEmail.copy().setCreateTimeToNow());
				inbox.setSendDevMsg(true);
				
//				EmailExecuteEvent.initDefaultCustomValue(inbox);
				MinecraftForge.EVENT_BUS.post(new EmailSendDevMessageEvent(player, inbox));
				EmailUtils.saveInboxToDisk(inbox);
			}
		}
	}
	
	@SubscribeEvent
	public static void onJoinWorld(EntityJoinLevelEvent event) {
		if(event.getEntity() instanceof ServerPlayer player) {
			Un un = Un.getInstance(player);
			if(un.unread > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnread(un.unread), player);
			}
			if(un.unReceived > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnreceived(un.unReceived), player);
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

			if(delay.msg >= 10 * 20){
				delay.msg = 0;
				Un un = Un.getInstance(player);
				if(un.unread>0 && un.unReceived>0){
					player.sendSystemMessage(Component.translatable("info.email.has_unread_and_unreceive", un.unread, un.unReceived), true);
				} else if (un.unread > 0) {
					player.sendSystemMessage(Component.translatable("info.email.has_unread", un.unread), true);
				} else if (un.unReceived > 0) {
					player.sendSystemMessage(Component.translatable("info.email.has_unreceive", un.unReceived), true);
				}
			}else {
				delay.msg++;
			}

			if(delay.net >= 10){
				Un un = Un.getInstance(player);
				if(un.unread != delay.unread) {
					delay.unread = un.unread;
					EmailMain.net.sendMessageToPlayer(new MsgUnread(un.unread), player);
				}
				if(un.unReceived != delay.unReceived) {
					delay.unReceived = un.unReceived;
					EmailMain.net.sendMessageToPlayer(new MsgUnreceived(un.unReceived), player);
				}
				delay.net = 0;
			}else {
				delay.net++;
			}
		}
	}

	static class Delay {
		int net = 0;
		int msg = 0;
		int unread = -1;
		int unReceived = -1;
	}
	record Un(int unread, int unReceived){
		static Un getInstance(Player player){
			Inbox inbox = Inbox.get(player);
			return new Un(inbox.getUnRead(), inbox.getUnReceived());
		}
	}

}
