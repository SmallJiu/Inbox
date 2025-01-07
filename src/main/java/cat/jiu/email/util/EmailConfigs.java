package cat.jiu.email.util;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public final class EmailConfigs {
//	public static final BooleanValue Enable_Inbox_Infinite_Storage_Cache;
	public static final BooleanValue Save_To_Minecraft_Root_Directory;
	public static final BooleanValue Save_Inbox_To_SQL;
//	public static final BooleanValue Screen_Inbox_Gui;
	public static final ConfigValue<String> Custom_Inbox_Path;
	public static final Layout Layout;
	public static class Layout extends BaseConfig {
		public final BooleanValue Enable_Chat_Button;
		public final IntValue Email_List_Width;
		public final BooleanValue Enable_Vanilla_Wrap_Text;
		public final Position Position;

		Layout(Builder builder) {
			super(builder);
			builder.translation("inbox.config.layout").push("main");
			this.Enable_Vanilla_Wrap_Text = builder
					.translation("inbox.config.layout.vanilla_wrap")
					.comment("inbox.config.layout.vanilla_wrap.0",
							"inbox.config.layout.vanilla_wrap.1")
					.define("Enable_Vanilla_Wrap_Text", true);

			this.Enable_Chat_Button = builder
					.translation("inbox.config.enable_chat_btn")
					.comment("inbox.config.enable_chat_btn.0")
					.define("Enable_Chat_Button", false);

			this.Email_List_Width = builder
					.translation("inbox.config.layout.email_list_width")
					.comment("inbox.config.layout.email_list_width.0")
					.defineInRange("Email_List_Width", 100, 0, Integer.MAX_VALUE);

			this.Position = new Position(builder);
			builder.pop();
		}

		public static class Position extends BaseConfig {
			public final InboxButtons Inbox_Buttons;
			Position(Builder builder) {
				super(builder);
				builder.translation("inbox.config.layout.pos").push("position");

				this.Inbox_Buttons = new InboxButtons(builder);

				builder.pop();
			}

			public static class InboxButtons extends BaseConfig {
				public final Pos Chat_Gui_Button;
				public final Pos Survival_Gui_Button;
				public final Pos Creative_Tab_Button;

				public InboxButtons(Builder builder) {
					super(builder);
					builder.translation("inbox.config.layout.pos.inbox_btn").push("inbox_buttons");

					this.Chat_Gui_Button = new Pos(builder, "chat_gui_button", "inbox.config.layout.pos.inbox_btn.chat_btn", 25, 5);
					this.Survival_Gui_Button = new Pos(builder, "survival_gui_button", "inbox.config.layout.pos.inbox_btn.survival_btn", 76, 54);
					this.Creative_Tab_Button = new Pos(builder, "creative_tab_button", "inbox.config.layout.pos.inbox_btn.creative_btn", 172, 166);

					builder.pop();
				}
			}
		}
	}

	public static final Send Send;
	public static class Send extends BaseConfig {
		public final BooleanValue Enable_Send_BlackList;
		public final BooleanValue Enable_Send_WhiteList;
		public final BooleanValue Enable_Send_To_Self;
		public final BooleanValue Enable_Send_Cooling;
		public final Cooling cooling;
		public final IntValue Send_History_Max_Count;

		Send(Builder builder) {
			super(builder);
			builder.translation("inbox.config.send").push("send");

			this.Enable_Send_BlackList = builder
					.translation("inbox.config.send.blacklist")
					.comment("inbox.config.send.blacklist.0",
							"inbox.config.send.blacklist.1")
					.define("Enable_Send_BlackList", false);

			this.Enable_Send_WhiteList = builder
					.translation("inbox.config.send.whitelist")
					.comment("inbox.config.send.whitelist.0",
							"inbox.config.send.whitelist.1")
					.define("Enable_Send_WhiteList", false);

			this.Enable_Send_To_Self = builder
					.translation("inbox.config.send.send_to_self")
					.comment("inbox.config.send.send_to_self.0")
					.define("Enable_Send_To_Self", false);

			this.Enable_Send_Cooling = builder
					.translation("inbox.config.send.cooling")
					.comment("inbox.config.send.cooling.0")
					.define("Enable_Send_Cooling", true);

			this.cooling = new Cooling(builder);

			this.Send_History_Max_Count = builder
					.translation("inbox.config.send.history_max")
					.comment("send history max count")
					.defineInRange("Send_History_Max_Count", 5, 0, Integer.MAX_VALUE);

			builder.pop();
		}

		public static class Cooling extends BaseConfig {
			public final IntValue Day;
			public final IntValue Hour;
			public final IntValue Minute;
			public final IntValue Second;
			public final IntValue Tick;
			public final IntValue Millis;
			Cooling(Builder builder) {
				super(builder);
				builder.translation("inbox.config.send.cooling").push("cooling");

				this.Day = builder
						.translation("inbox.config.time.day")
						.comment("inbox.config.send.cooling.day")
						.defineInRange("Day", 0, 0, Integer.MAX_VALUE);

				this.Hour = builder
						.translation("inbox.config.time.hour")
						.comment("inbox.config.send.cooling.hour")
						.defineInRange("Hour", 0, 0, Integer.MAX_VALUE);

				this.Minute = builder
						.translation("inbox.config.time.minute")
						.comment("inbox.config.send.cooling.minute")
						.defineInRange("Minute", 0, 0, Integer.MAX_VALUE);

				this.Second = builder
						.translation("inbox.config.time.second")
						.comment("inbox.config.send.cooling.second")
						.defineInRange("Second", 5, 0, Integer.MAX_VALUE);

				this.Tick = builder
						.translation("inbox.config.time.tick")
						.comment("inbox.config.send.cooling.tick")
						.defineInRange("Tick", 0, 0, Integer.MAX_VALUE);

				this.Millis = builder
						.translation("inbox.config.time.millis")
						.comment("inbox.config.send.cooling.millis")
						.defineInRange("Millis", 0, 0, Integer.MAX_VALUE);

				builder.pop();
			}
		}
	}

	public static final ForgeConfigSpec CONFIG_MAIN;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
//		Enable_Inbox_Infinite_Storage_Cache = builder
//				.translation("inbox.config.infinite_size")
//				.comment("inbox.config.infinite_size.0",
//						"inbox.config.infinite_size.1")
//				.define("Enable_Inbox_Infinite_Storage_Cache", false);

		Save_To_Minecraft_Root_Directory = builder
				.worldRestart()
				.translation("inbox.config.save_to_root_directory")
				.comment("inbox.config.save_to_root_directory.0",
						"inbox.config.save_to_root_directory.1")
				.define("Save_To_Minecraft_Root_Directory", false);

		Save_Inbox_To_SQL = builder
				.worldRestart()
				.translation("inbox.config.save_inbox_to_sql")
				.comment("inbox.config.save_inbox_to_sql.0",
						"inbox.config.save_inbox_to_sql.1")
				.define("Save_Inbox_To_SQL", false);

//		Screen_Inbox_Gui = builder
//				.worldRestart()
//				.translation("inbox.config.screen_inbox_gui")
//				.comment("inbox.config.screen_inbox_gui.0",
//						"inbox.config.screen_inbox_gui.1")
//				.define("Screen_Inbox_Gui", true);

		Custom_Inbox_Path = builder
				.worldRestart()
				.translation("inbox.config.custom_inbox_path")
				.comment("inbox.config.custom_inbox_path.0",
						"inbox.config.custom_inbox_path.1",
						"inbox.config.custom_inbox_path.2",
						"inbox.config.custom_inbox_path.3",
						"inbox.config.custom_inbox_path.4")
				.define("inbox_path", "");


		Layout = new Layout(builder);
		Send = new Send(builder);

//		builder.pop();
		CONFIG_MAIN = builder.build();
	}

	public static class Pos extends BaseConfig {
		public final IntValue X;
		public final IntValue Y;
		public Pos(Builder builder, String pathName, int x, int y, String... comments) {
			this(builder, pathName, null,  x, y, comments);
		}
		public Pos(Builder builder, String pathName, String translation, int x, int y, String... comments) {
			super(builder);
			builder.translation(translation).push(pathName);
			this.X = builder
					.translation("inbox.config.pos.x")
					.comment(comments)
					.defineInRange("X", x, Integer.MIN_VALUE, Integer.MAX_VALUE);
			this.Y = builder
					.translation("inbox.config.pos.y")
					.comment(comments)
					.defineInRange("Y", y, Integer.MIN_VALUE, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class BaseConfig {
		public BaseConfig(@SuppressWarnings("unused") Builder builder) {}
	}

	@Deprecated
	public static boolean isInfiniteSize() {
		return false;
//		return EmailMain.proxy.isClient()
//			&& Minecraft.getInstance().isIntegratedServerRunning()
//			&& EmailConfigs.Enable_Inbox_Infinite_Storage_Cache.get();
	}
}
