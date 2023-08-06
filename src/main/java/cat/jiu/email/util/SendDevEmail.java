package cat.jiu.email.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cat.jiu.email.net.msg.MsgUnreceived;
import com.google.common.collect.Lists;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Sound;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendDevMessageEvent;
import cat.jiu.email.net.msg.MsgPlayerPermissionLevel;
import cat.jiu.email.net.msg.MsgUnread;

import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
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
		msgs.add(new Text("email.dev_message.0", ""));
		for(int i = 1; i < 30; i++) {
			msgs.add(new Text("email.dev_message."+i));
		}
		devEmail = new Email(new Text("email.dev_message.title", ""), new Text("email.dev_message.sender"),
				new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundCategory.PLAYERS),
				Arrays.asList(new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 8)), msgs);
		devEmail.setExpirationTime(new TimeMillis(9999, 23, 59, 59, 9999))
				.setAccept(true);
	}
	
	public static Email getDevEmail() {
		return devEmail;
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if(!event.getPlayer().getEntityWorld().isRemote()) {
			Inbox inbox = Inbox.get(event.getPlayer());
			
			int level = 0;
			OpEntry opEntry = event.getPlayer().getServer().getPlayerList().getOppedPlayers().getEntry(event.getPlayer().getGameProfile());
            if (opEntry != null) {
                level = opEntry.getPermissionLevel();
            }
			
			EmailMain.net.sendMessageToPlayer(new MsgPlayerPermissionLevel(level), (ServerPlayerEntity) event.getPlayer());
			
			if(!inbox.isSendDevMsg()) {
				devEmail.setMessageParameters(0, 0, event.getPlayer().getName().getString());
				devEmail.setTitleParameters(0, event.getPlayer().getName().getString());
				devEmail.setCreateTimeToNow();
				
				inbox.addEmail(devEmail);
				inbox.setSendDevMsg(true);
				
//				EmailExecuteEvent.initDefaultCustomValue(inbox);
				MinecraftForge.EVENT_BUS.post(new EmailSendDevMessageEvent((ServerPlayerEntity) event.getPlayer(), inbox));
				EmailUtils.saveInboxToDisk(inbox);
			}
		}
	}

	@SubscribeEvent
	public static void onJoinWorld(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
			Inbox inbox = Inbox.get(player);
			int unread = inbox.getUnRead();
			int unReceived = inbox.getUnReceived();
			if(unread > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnread(unread), player);
			}
			if(unReceived > 0) {
				EmailMain.net.sendMessageToPlayer(new MsgUnreceived(unReceived), player);
			}
		}
	}

	private static final Map<String, Delay> reminds = Maps.newHashMap();
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event){
		if(event.player instanceof ServerPlayerEntity && event.phase == TickEvent.Phase.END){
			ServerPlayerEntity player = (ServerPlayerEntity) event.player;
			String name = player.getName().getString();
			if(!reminds.containsKey(name)) reminds.put(name, new Delay());
			Delay delay = reminds.get(name);

			if(delay.msg >= 10 * 20){
				delay.msg = 0;
				Inbox inbox = Inbox.get(player);
				int unread = inbox.getUnRead();
				int unaccepted = inbox.getUnReceived();
				if(unread>0 && unaccepted>0){
					player.sendStatusMessage(new TranslationTextComponent("info.email.has_unread_and_unreceive", unread, unaccepted), true);
				} else if (unread > 0) {
					player.sendStatusMessage(new TranslationTextComponent("info.email.has_unread", unread), true);
				} else if (unaccepted > 0) {
					player.sendStatusMessage(new TranslationTextComponent("info.email.has_unreceive", unaccepted), true);
				}
			}else {
				delay.msg++;
			}

			if(delay.net >= 10){
				Inbox inbox = Inbox.get(player);
				int unread = inbox.getUnRead();
				int unReceived = inbox.getUnReceived();
				if(delay.unread != unread) {
					delay.unread = unread;
					EmailMain.net.sendMessageToPlayer(new MsgUnread(unread), player);
				}
				if(delay.unReceived != unReceived) {
					delay.unReceived = unReceived;
					EmailMain.net.sendMessageToPlayer(new MsgUnreceived(unReceived), player);
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
}
