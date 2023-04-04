package cat.jiu.email.util;

import java.util.List;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.InboxSound;
import cat.jiu.email.element.InboxText;
import cat.jiu.email.element.InboxTime;
import cat.jiu.email.event.EmailSendDevMessageEvent;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.net.msg.MsgUnread;
import cat.jiu.email.net.msg.MsgUnreceive;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber
public class SendDevEmail {
	static final Email devEmail;
	static {
		List<IInboxText> msgs = Lists.newArrayList();
		msgs.add(new InboxText("email.dev_message.0", ""));
		for(int i = 1; i < 7; i++) {
			msgs.add(new InboxText("email.dev_message."+i));
		}
		devEmail = new Email(new InboxText("email.dev_message.title", ""), new InboxText("email.dev_message.sender"),
				new InboxSound(new InboxTime(2,58,0), SoundEvents.RECORD_11, 1, 1), 
				null, msgs);
		devEmail.setExpirationTime(new TimeMillis(EmailUtils.parseMillis(10, 0, 0, 0, 0)));
	}
	
	@SubscribeEvent
	public static void onJoin(PlayerLoggedInEvent event) {
		if(!event.player.world.isRemote) {
			Inbox inbox = Inbox.get(event.player);
			if(!inbox.isSendDevMsg()) {
				inbox.setSendDevMsg(true);
				
				devEmail.getMsgs().get(0).getParameters()[0] = event.player.getName();
				devEmail.getTitle().getParameters()[0] = event.player.getName();
				devEmail.setCreateTimeToNow();
				
				inbox.addEmail(devEmail);
				
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
					EmailMain.execute(args->{
						EmailMain.setUnread(unread);
					}, 100);
				}
			}
			if(unaccept > 0) {
				if(!EmailMain.proxy.isClient()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnreceive(unaccept), (EntityPlayerMP) event.getEntity());
				}else {
					EmailMain.execute(args->{
						EmailMain.setAccept(unaccept);
					},100);
				}
			}
		}
	}
}
