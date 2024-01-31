package cat.jiu.email.util;

import java.util.Arrays;
import java.util.List;

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
import cat.jiu.email.net.msg.MsgUnreceive;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

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
				new Sound(new Timer(3,6,0), SoundEvents.RECORD_CAT, 1, 1, SoundCategory.PLAYERS), 
				Arrays.asList(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND, 4),
						new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND, 4)), msgs);
		devEmail.setExpirationTime(new TimeMillis(114514, 23, 59, 59, 999));
		devEmail.setAccept(true);
	}
	
	public static Email getDevEmail() {
		return devEmail;
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerLoggedInEvent event) {
		if(!event.player.world.isRemote) {
			Inbox inbox = Inbox.get(event.player);
			
			int level = 0;
			UserListOpsEntry userlistopsentry = event.player.getServer().getPlayerList().getOppedPlayers().getEntry(event.player.getGameProfile());
            if (userlistopsentry != null) {
                level = userlistopsentry.getPermissionLevel();
            }
			
			EmailMain.net.sendMessageToPlayer(new MsgPlayerPermissionLevel(level), (EntityPlayerMP) event.player);
			
			if(!inbox.isSendDevMsg()) {
				devEmail.setMessageParameters(0, 0, event.player.getName());
				devEmail.setTitleParameters(0, event.player.getName());
				devEmail.setCreateTimeToNow();
				
				inbox.addEmail(devEmail);
				inbox.setSendDevMsg(true);
				
				EmailExecuteEvent.initDefaultCustomValue(inbox);
				MinecraftForge.EVENT_BUS.post(new EmailSendDevMessageEvent(event.player, inbox));
				EmailUtils.saveInboxToDisk(inbox);
			}
		}
	}
	
	@SubscribeEvent
	public static void onJoinWorld(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof EntityPlayer && !event.getWorld().isRemote) {
			Inbox inbox = Inbox.get((EntityPlayer)event.getEntity());
			int unread = inbox.getUnRead();
			int unaccept = inbox.getUnReceived();
			if(unread > 0) {
				if(!EmailMain.proxy.isClient()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) event.getEntity());
				}else {
					EmailMain.setUnread(unread);
				}
			}
			if(unaccept > 0) {
				if(!EmailMain.proxy.isClient()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnreceive(unaccept), (EntityPlayerMP) event.getEntity());
				}else {
					EmailMain.setAccept(unaccept);
				}
			}
		}
	}
}
