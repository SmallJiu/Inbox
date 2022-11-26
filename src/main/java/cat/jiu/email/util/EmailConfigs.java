package cat.jiu.email.util;

import cat.jiu.email.EmailMain;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(
	modid = EmailMain.MODID,
	name = "jiu/" + EmailMain.MODID + "/main",
	category = "config_main")
@Mod.EventBusSubscriber(modid = EmailMain.MODID)
public final class EmailConfigs {
	@Config.LangKey("email.config.infinite_size")
	@Config.Comment("enable mailbox infinite storage cache, \n"
				 + "§cWarning: only enable on Single player.")
	public static boolean Enable_MailBox_Infinite_Storage_Cache = false;
	
	@Config.RequiresWorldRestart
	@Config.LangKey("email.config.save_to_root_directory")
	@Config.Comment("save email to minecraft root directory\n"
			+ "save email to world directory if false\n"
			+ "can cross you saves send email if true")
	public static boolean Save_To_Minecraft_Root_Directory = false;
	
	@Config.LangKey("email.config.main")
	@Config.Comment("email main")
	public static Main Main = new Main();
	public static class Main {
		@Config.LangKey("email.config.main.show_text_rows")
		@Config.Comment("selected text rows")
		@Config.RangeInt(min = 1, max = 8)
		public int Selected_Text_Rows = 6;
		
		@Config.LangKey("email.config.main.show_text_spacing")
		@Config.Comment("selected text spacing")
		@Config.RangeInt(min = 9)
		public int Selected_Text_Spacing = 12;
		
		@Config.LangKey("email.config.main.size")
		@Config.Comment("inbox gui size")
		public Size Size = new Size();
		public class Size {
			@Config.LangKey("email.config.main.size.width")
			@Config.Comment("gui width")
			public int Width = 230;
			@Config.LangKey("email.config.main.size.height")
			@Config.Comment("gui height")
			public int Height = 164;
		}
		
		@Config.LangKey("email.config.main.pos")
		@Config.Comment("text position")
		public Position Position = new Position();
		public class Position {
			@Config.LangKey("email.config.main.number_of_words.current")
			@Config.Comment("current email position")
			public CurrentEmail Current_Email = new CurrentEmail();
			public class CurrentEmail {
				@Config.LangKey("email.config.main.pos.row")
				@Config.Comment("row position")
				public Row Row = new Row();
				public class Row {
					public int X = 93;
					public int Y = 33;
				}
				@Config.LangKey("email.config.msg")
				@Config.Comment("message position")
				public Message Msg = new Message();
				public class Message {
					public int X = 101;
					public int Y = 33;
				}
				@Config.LangKey("email.config.sender")
				@Config.Comment("sender position")
				public Sender Sender = new Sender();
				public class Sender {
					public int X = 88;
					public int Y = 20;
				}
				@Config.LangKey("email.config.main.pos.current.msgid")
				@Config.Comment("current msg ID position")
				public MsgID MsgID = new MsgID();
				public class MsgID {
					public int X = 80;
					public int Y = 6;
				}
				@Config.LangKey("email.config.main.pos.current.items")
				@Config.Comment("items position")
				public Items Items = new Items();
				public class Items {
					public int X = 45;
					public int Y = 109;
				}
				@Config.LangKey("email.config.title")
				@Config.Comment("title position")
				public Title Title = new Title();
				public class Title {
					public int X = 88;
					public int Y = 6;
					@Config.LangKey("email.config.main.pos.title.time")
					@Config.Comment("send time position")
					public Time Time = new Time();
					public class Time {
						public int X = 161;
						public int Y = 20;
					}
				}
			}
			
			@Config.LangKey("email.config.main.number_of_words.candidate")
			@Config.Comment("candidate msgs")
			public CandidateEmail Candidate_Email = new CandidateEmail();
			public class CandidateEmail {
				public int X = 18;
				public int Y = 19;
			}
		}
		
		@Config.LangKey("email.config.main.number_of_words")
		@Config.Comment("Number of words")
		public NumberOfWords Number_Of_Words = new NumberOfWords(); 
		public class NumberOfWords {
			@Config.LangKey("email.config.main.number_of_words.current")
			@Config.Comment("current message")
			public CurrentEmail Current_Email = new CurrentEmail();
			public class CurrentEmail {
				@Config.LangKey("email.config.msg")
				@Config.RangeInt(min = 1)
				public int Message = 106;
				@Config.LangKey("email.config.title")
				@Config.RangeInt(min = 1)
				public int Title = 125;
				@Config.LangKey("email.config.sender")
				@Config.RangeInt(min = 1)
				public int Sender = 61;
			}
			@Config.LangKey("email.config.main.number_of_words.candidate")
			@Config.Comment("candidate msgs")
			public CandidateEmail Candidate_Email = new CandidateEmail();
			public class CandidateEmail {
				@Config.LangKey("email.config.sender")
				@Config.RangeInt(min = 1)
				public int Sender = 37;
			}
		}
	}
	
	@Config.LangKey("email.config.send")
	@Config.Comment("email send")
	public static Send Send = new Send();
	public static class Send {
		@Config.LangKey("email.config.send.blacklist")
		@Config.Comment("enable send email black list, \n"
					+ "§cCannot enable at the same time as White List")
		public boolean Enable_Send_BlackList = false;
		
		@Config.LangKey("email.config.send.whitelist")
		@Config.Comment("enable send email white list, \n"
					+ "§cCannot enable at the same time as Black List")
		public boolean Enable_Send_WhiteList = false;
		
		@Config.LangKey("email.config.send.send_to_self")
		@Config.Comment("enable send email to self")
		public boolean Enable_Send_To_Self = false;
		
		@Config.LangKey("email.config.send.cooling")
		@Config.Comment("enable send email need cooling")
		public boolean Enable_Send_Cooling = true;
		
		@Config.LangKey("email.config.send.inbox_button")
		@Config.Comment("enable send email gui inbox button")
		public boolean Enable_Inbox_Button = false;
		
		@Config.LangKey("email.config.send.cooling.time")
		@Config.Comment("send email cooling time")
		public Cooling cooling = new Cooling();
		public class Cooling {
			@Config.LangKey("email.config.send.cooling.time.day")
			@Config.Comment("send cooling of day")
			@Config.RangeInt(min = 0)
			public int Day = 0;
			
			@Config.LangKey("email.config.send.cooling.time.hour")
			@Config.Comment("send cooling of hour")
			@Config.RangeInt(min = 0)
			public int Hour = 0;
			
			@Config.LangKey("email.config.send.cooling.time.minute")
			@Config.Comment("send cooling of minute")
			@Config.RangeInt(min = 0)
			public int Minute = 0;
			
			@Config.LangKey("email.config.send.cooling.time.second")
			@Config.Comment("send cooling of second")
			@Config.RangeInt(min = 2)
			public int Second = 5;
		
			@Config.LangKey("email.config.send.cooling.time.tick")
			@Config.Comment("send cooling of tick")
			@Config.RangeInt(min = 0)
			public int Tick = 0;
		}
	}
	
	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if(event.getModID().equals(EmailMain.MODID)) {
			if(Send.Enable_Send_BlackList && Send.Enable_Send_WhiteList) {
				Send.Enable_Send_BlackList = false;
				Send.Enable_Send_WhiteList = false;
			}
			ConfigManager.sync(EmailMain.MODID, Config.Type.INSTANCE);
		}
	}
}
