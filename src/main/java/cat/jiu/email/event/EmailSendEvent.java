package cat.jiu.email.event;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import cat.jiu.email.util.EmailUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class EmailSendEvent {
	@Cancelable
	public static class Pre extends Event {
		public final MinecraftServer server;
		private String title;
		private String time;
		private String sender;
		public final EmailSenderGroup group;
		public final String addresserName;
		public final UUID addresserUID;
		public final List<String> messages;
		public final List<ItemStack> items;
		
		public Pre(MinecraftServer server, String title, String time, String sender, EmailSenderGroup group, String addresserName, UUID addresserUID, List<String> messages, List<ItemStack> items) {
			this.server = server;
			this.title = title;
			this.time = time;
			this.sender = sender;
			this.group = group;
			this.addresserName = addresserName;
			this.addresserUID = addresserUID;
			this.messages = messages;
			this.items = items;
		}
		
		public String getTitle() {return title;}
		public void setTitle(String title) {this.title = title;}
		
		public String getSendTime() {return time;}
		public void setSendTime(String time) {this.time = time;}
		
		public String getSender() {return sender;}
		public void setSender(String sender) {this.sender = sender;}
	}
	
	public static class Post extends Event {
		public final MinecraftServer server;
		public final String sender;
		public final EmailSenderGroup group;
		public final String addresser;
		public final UUID addresserUUID;
		public final JsonObject email;
		
		public Post(MinecraftServer server, String sender, EmailSenderGroup group, String addresser, UUID addresserUUID, JsonObject email) {
			this.server = server;
			this.sender = sender;
			this.group = group;
			this.addresser = addresser;
			this.addresserUUID = addresserUUID;
			this.email = EmailUtils.copyJson(email);
		}
	}
	
	public static enum EmailSenderGroup {
		SYSTEM, PLAYER, COMMAND;
		public static EmailSenderGroup getGroupByID(int id) {
			switch(id) {
				case 1: return PLAYER;
				case 2: return COMMAND;
				default: return SYSTEM;
			}
		}
		public static int getIDByGroup(EmailSenderGroup sender) {
			switch(sender) {
				case PLAYER: return 1;
				case COMMAND: return 2;
				default: return 0;
			}
		}
		public boolean isPlayerSend() {
			return this == PLAYER;
		}
		public boolean isCommandSend() {
			return this == COMMAND;
		}
		public boolean isSystemSend() {
			return this == SYSTEM;
		}
	}
}
