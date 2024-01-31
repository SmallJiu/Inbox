package cat.jiu.email.util;

import cat.jiu.email.EmailMain;

import net.minecraft.client.Minecraft;

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
	@Config.Comment("enable inbox infinite storage cache, \n"
				 + "§cWarning: only enable on Single player.")
	public static boolean Enable_Inbox_Infinite_Storage_Cache = false;
	
	@Config.RequiresWorldRestart
	@Config.LangKey("email.config.save_to_root_directory")
	@Config.Comment("save email to minecraft root directory\n"
			+ "save email to world directory if false\n"
			+ "can cross you saves send email if true")
	public static boolean Save_To_Minecraft_Root_Directory = false;
	
	@Config.RequiresWorldRestart
	@Config.LangKey("email.config.save_inbox_to_sql")
	@Config.Comment("save inbox to sql file(inboxs.db)\n"
			+ "§cWarning: you need backup old inbox files!")
	public static boolean Save_Inbox_To_SQL = false;
	
	@Config.RequiresWorldRestart
	@Config.LangKey("email.config.custom_inbox_path")
	@Config.Comment({
		"Customize the path to save the mailbox, default to the corresponding archive folder",
		"It can be an absolute path or a relative path,",
		"Except for higher version minecraft items, the rest are compatible with lower version email",
		"Example: 'C:/' inbox will be saved in 'C:/email/<uuid>.json' instead of 'C:/<uuid>.json'. If 'Save to SQL' is enabled, it will be saved in 'C:/inboxes.db'",
		"Example: '/' The 'data' inbox will be saved in '/Data/email/<uuid>.json 'instead of'/Data/<uuid>.json ', if' Save to SQL 'is enabled, it will be saved to' C:/inboxes.db '"
	})
	public static String Custom_Inbox_Path = "";
	
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
		@Config.RangeInt(min = 0)
		public int Selected_Text_Spacing = 3;
		
		@Config.LangKey("email.config.main.vanilla_wrap")
		@Config.Comment("use vanilla to wrap text if true, else will use Single char wrap.")
		public boolean Enable_Vanilla_Wrap_Text = true;
		
		@Config.LangKey("email.config.main.size")
		@Config.Comment("inbox gui size")
		public Size Size = new Size();
		public class Size {
			@Config.LangKey("email.config.main.size.width")
			@Config.Comment("gui width")
			public int Width = 236;
			@Config.LangKey("email.config.main.size.height")
			@Config.Comment("gui height")
			public int Height = 168;
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
				public Pos Row = new Pos(93, 33);
				
				@Config.LangKey("email.config.msg")
				@Config.Comment("message position")
				public Pos Msg = new Pos(101, 33);
				
				@Config.LangKey("email.config.sender")
				@Config.Comment("sender position")
				public Pos Sender = new Pos(88, 20);
				
				@Config.LangKey("email.config.main.pos.current.msgid")
				@Config.Comment("current msg ID position")
				public Pos MsgID = new Pos(80, 6);
				
				@Config.LangKey("email.config.main.pos.current.items")
				@Config.Comment("items position")
				public Pos Items = new Pos(48, 109);
				
				@Config.LangKey("email.config.title")
				@Config.Comment("title position")
				public Title Title = new Title();
				public class Title {
					public int X = 88;
					public int Y = 6;
					@Config.LangKey("email.config.main.pos.title.time")
					@Config.Comment("send time position")
					public Pos Time = new Pos(161, 20);
				}
			}
			
			@Config.LangKey("email.config.main.number_of_words.candidate")
			@Config.Comment("candidate msgs")
			public Pos Candidate_Email = new Pos(18, 11);
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
				public int Sender = 44;
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

		@Config.LangKey("email.config.send.history_max")
		@Config.Comment("send history max count")
		public int Send_History_Max_Count = 5;

		@Config.LangKey("email.config.send.cooling.time")
		@Config.Comment("send email cooling time")
		public Cooling cooling = new Cooling();
		public class Cooling {
			@Config.LangKey("email.config.time.day")
			@Config.Comment("send cooling of day")
			@Config.RangeInt(min = 0)
			public int Day = 0;
			
			@Config.LangKey("email.config.time.hour")
			@Config.Comment("send cooling of hour")
			@Config.RangeInt(min = 0)
			public int Hour = 0;
			
			@Config.LangKey("email.config.time.minute")
			@Config.Comment("send cooling of minute")
			@Config.RangeInt(min = 0)
			public int Minute = 0;
			
			@Config.LangKey("email.config.time.second")
			@Config.Comment("send cooling of second")
			@Config.RangeInt(min = 2)
			public int Second = 5;
		
			@Config.LangKey("email.config.time.tick")
			@Config.Comment("send cooling of tick")
			@Config.RangeInt(min = 0)
			public int Tick = 0;
			
			@Config.LangKey("email.config.time.millis")
			@Config.Comment("send cooling of millis")
			@Config.RangeInt(min = 0)
			public int Millis = 0;
		}
	}
	
	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if(EmailMain.MODID.equals(event.getModID())) {
			ConfigManager.sync(EmailMain.MODID, Config.Type.INSTANCE);
		}
	}
	
	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
		if(EmailMain.MODID.equals(event.getModID())) {
			if(Send.Enable_Send_BlackList && Send.Enable_Send_WhiteList) {
				Send.Enable_Send_BlackList = false;
				Send.Enable_Send_WhiteList = false;
			}
		}
	}
	
	public static class Pos {
		public int X;
		public int Y;
		public Pos(int x, int y) {
			this.X = x;
			this.Y = y;
		}
	}
	
	public static boolean isInfiniteSize() {
		return EmailMain.proxy.isClient()
			&& Minecraft.getMinecraft().isSingleplayer()
			&& EmailConfigs.Enable_Inbox_Infinite_Storage_Cache;
	}
}
