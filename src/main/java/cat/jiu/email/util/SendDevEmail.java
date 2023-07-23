package cat.jiu.email.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
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
		for(int i = 1; i < 7; i++) {
			msgs.add(new Text("email.dev_message."+i));
		}
		devEmail = new Email(new Text("email.dev_message.title", ""), new Text("email.dev_message.sender"),
				new Sound(new Timer(3,6,0), SoundEvents.MUSIC_DISC_CAT, 1, 1, SoundCategory.PLAYERS),
				Arrays.asList(new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.DIAMOND, 8)), msgs);
		devEmail.setAccept(true);
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
		if(event.getEntity() instanceof PlayerEntity && !event.getWorld().isRemote()) {
			Inbox inbox = Inbox.get((PlayerEntity) event.getEntity());
			int unread = inbox.getUnRead();
			int unaccept = inbox.getUnReceived();
			if(unread > 0) {
				if(!EmailMain.proxy.isClient()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnread(unread), (ServerPlayerEntity) event.getEntity());
				}else {
					EmailMain.execute(()->{
						EmailMain.setUnread(unread);
					}, 100);
				}
			}
			if(unaccept > 0) {
				if(!EmailMain.proxy.isClient()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnreceived(unaccept), (ServerPlayerEntity) event.getEntity());
				}else {
					EmailMain.execute(()->{
						EmailMain.setAccept(unaccept);
					},100);
				}
			}
		}
	}
}
