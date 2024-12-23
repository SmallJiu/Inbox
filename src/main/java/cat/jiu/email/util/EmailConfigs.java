package cat.jiu.email.util;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public final class EmailConfigs {
//	public static final BooleanValue Enable_Inbox_Infinite_Storage_Cache;
	public static final BooleanValue Save_To_Minecraft_Root_Directory;
	public static final BooleanValue Save_Inbox_To_SQL;
//	public static final BooleanValue Screen_Inbox_Gui;
	public static final ConfigValue<String> Custom_Inbox_Path;
	public static final Main Main;
	public static class Main extends BaseConfig {
		public final BooleanValue Enable_Chat_Button;
		public final IntValue Selected_Text_Rows;
		public final IntValue Selected_Text_Spacing;
		public final BooleanValue Enable_Vanilla_Wrap_Text;
		public final Position Position;

		Main(Builder builder) {
			super(builder);
			builder.translation("email.config.main").push("main");

			this.Enable_Chat_Button = builder
					.translation("email.config.enable_chat_btn")
					.comment("email.config.enable_chat_btn.0")
					.define("Enable_Chat_Button", false);
			this.Selected_Text_Rows = builder
					.translation("email.config.main.show_text_rows")
					.comment("email.config.main.show_text_rows.0")
					.defineInRange("Selected_Text_Rows", 6, 1, 8);
			this.Selected_Text_Spacing = builder
					.translation("email.config.main.show_text_spacing")
					.comment("email.config.main.show_text_spacing.0")
					.defineInRange("Selected_Text_Spacing", 3, 0, Integer.MAX_VALUE);
			this.Enable_Vanilla_Wrap_Text = builder
					.translation("email.config.main.vanilla_wrap")
					.comment("email.config.main.vanilla_wrap.0",
							"email.config.main.vanilla_wrap.1")
					.define("Enable_Vanilla_Wrap_Text", true);
			this.Position = new Position(builder);
			builder.pop();
		}

		public static class Position extends BaseConfig {
			public final InboxButtons Inbox_Buttons;
			Position(Builder builder) {
				super(builder);
				builder.translation("email.config.main.pos").push("position");

				this.Inbox_Buttons = new InboxButtons(builder);

				builder.pop();
			}

			public static class InboxButtons extends BaseConfig {
				public final Pos Chat_Gui_Button;
				public final Pos Survival_Gui_Button;
				public final Pos Creative_Tab_Button;

				public InboxButtons(Builder builder) {
					super(builder);
					builder.translation("email.config.main.pos.inbox_btn").push("inbox_buttons");

					this.Chat_Gui_Button = new Pos(builder, "chat_gui_button", "email.config.main.pos.inbox_btn.chat_btn", 25, 5);
					this.Survival_Gui_Button = new Pos(builder, "survival_gui_button", "email.config.main.pos.inbox_btn.survival_btn", 76, 54);
					this.Creative_Tab_Button = new Pos(builder, "creative_tab_button", "email.config.main.pos.inbox_btn.creative_btn", 172, 166);

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
			builder.translation("email.config.send").push("send");

			this.Enable_Send_BlackList = builder
					.translation("email.config.send.blacklist")
					.comment("email.config.send.blacklist.0",
							"email.config.send.blacklist.1")
					.define("Enable_Send_BlackList", false);

			this.Enable_Send_WhiteList = builder
					.translation("email.config.send.whitelist")
					.comment("email.config.send.whitelist.0",
							"email.config.send.whitelist.1")
					.define("Enable_Send_WhiteList", false);

			this.Enable_Send_To_Self = builder
					.translation("email.config.send.send_to_self")
					.comment("email.config.send.send_to_self.0")
					.define("Enable_Send_To_Self", false);

			this.Enable_Send_Cooling = builder
					.translation("email.config.send.cooling")
					.comment("email.config.send.cooling.0")
					.define("Enable_Send_Cooling", true);

			this.cooling = new Cooling(builder);

			this.Send_History_Max_Count = builder
					.translation("email.config.send.history_max")
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
				builder.translation("email.config.send.cooling").push("cooling");

				this.Day = builder
						.translation("email.config.time.day")
						.comment("email.config.send.cooling.day")
						.defineInRange("Day", 0, 0, Integer.MAX_VALUE);

				this.Hour = builder
						.translation("email.config.time.hour")
						.comment("email.config.send.cooling.hour")
						.defineInRange("Hour", 0, 0, Integer.MAX_VALUE);

				this.Minute = builder
						.translation("email.config.time.minute")
						.comment("email.config.send.cooling.minute")
						.defineInRange("Minute", 0, 0, Integer.MAX_VALUE);

				this.Second = builder
						.translation("email.config.time.second")
						.comment("email.config.send.cooling.second")
						.defineInRange("Second", 5, 0, Integer.MAX_VALUE);

				this.Tick = builder
						.translation("email.config.time.tick")
						.comment("email.config.send.cooling.tick")
						.defineInRange("Tick", 0, 0, Integer.MAX_VALUE);

				this.Millis = builder
						.translation("email.config.time.millis")
						.comment("email.config.send.cooling.millis")
						.defineInRange("Millis", 0, 0, Integer.MAX_VALUE);

				builder.pop();
			}
		}
	}

	public static final ForgeConfigSpec CONFIG_MAIN;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
//		Enable_Inbox_Infinite_Storage_Cache = builder
//				.translation("email.config.infinite_size")
//				.comment("email.config.infinite_size.0",
//						"email.config.infinite_size.1")
//				.define("Enable_Inbox_Infinite_Storage_Cache", false);

		Save_To_Minecraft_Root_Directory = builder
				.worldRestart()
				.translation("email.config.save_to_root_directory")
				.comment("email.config.save_to_root_directory.0",
						"email.config.save_to_root_directory.1")
				.define("Save_To_Minecraft_Root_Directory", false);

		Save_Inbox_To_SQL = builder
				.worldRestart()
				.translation("email.config.save_inbox_to_sql")
				.comment("email.config.save_inbox_to_sql.0",
						"email.config.save_inbox_to_sql.1")
				.define("Save_Inbox_To_SQL", false);

//		Screen_Inbox_Gui = builder
//				.worldRestart()
//				.translation("email.config.screen_inbox_gui")
//				.comment("email.config.screen_inbox_gui.0",
//						"email.config.screen_inbox_gui.1")
//				.define("Screen_Inbox_Gui", true);

		Custom_Inbox_Path = builder
				.worldRestart()
				.translation("email.config.custom_inbox_path")
				.comment("email.config.custom_inbox_path.0",
						"email.config.custom_inbox_path.1",
						"email.config.custom_inbox_path.2",
						"email.config.custom_inbox_path.3",
						"email.config.custom_inbox_path.4")
				.define("inbox_path", "");


		Main = new Main(builder);
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
					.translation("email.config.pos.x")
					.comment(comments)
					.defineInRange("X", x, Integer.MIN_VALUE, Integer.MAX_VALUE);
			this.Y = builder
					.translation("email.config.pos.y")
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
