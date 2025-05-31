package cat.jiu.email.util;

import cat.jiu.core.util.client.config.BaseConfig;
import cat.jiu.email.element.StorageType;
import cat.jiu.sql.SQLDatabaseDriver;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public final class EmailConfigs {
	public static final BooleanValue Save_To_Minecraft_Root_Directory;
	public static final ConfigValue<String> Custom_Inbox_Path;
	public static final ConfigValue<String> Storage_Inbox_Types;
	public static final SQLProperties SQL_PROPERTIES;
	public static final Layout Layout;
	public static class Layout extends BaseConfig {
		public final BooleanValue Enable_Chat_Button;
		public final IntValue Email_List_Width;
		public final BooleanValue Enable_Vanilla_Wrap_Text;
		public final BooleanValue Lock_Inbox_Button_Dragging;
		public final Position Position;
		public final Time Prompt_Email;

		Layout(Builder builder) {
			super(builder);
			builder.translation("inbox.config.layout").push("main");
			this.Enable_Vanilla_Wrap_Text = builder
					.translation("inbox.config.layout.vanilla_wrap")
					.comment("inbox.config.layout.vanilla_wrap.0",
							"inbox.config.layout.vanilla_wrap.1")
					.define("Enable_Vanilla_Wrap_Text", true);

			this.Lock_Inbox_Button_Dragging = builder
					.translation("inbox.config.layout.button_dragging")
					.comment("inbox.config.layout.button_dragging.0")
					.define("Lock_Inbox_Button_Dragging", false);

			this.Enable_Chat_Button = builder
					.translation("inbox.config.enable_chat_btn")
					.comment("inbox.config.enable_chat_btn.0")
					.define("Enable_Chat_Button", false);

			this.Email_List_Width = builder
					.translation("inbox.config.layout.email_list_width")
					.comment("inbox.config.layout.email_list_width.0")
					.defineInRange("Email_List_Width", 100, 0, Integer.MAX_VALUE);

			this.Position = new Position(builder);
			this.Prompt_Email = new Time(builder, "inbox.config.layout.prompt", "prompt", 0, 0, 0, 25, 0, 0);
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
				public final DoubleValue Chat_Gui_Button_Size;
				public final DoubleValue Survival_Gui_Button_Size;
				public final DoubleValue Creative_Tab_Button_Size;

				public InboxButtons(Builder builder) {
					super(builder);
					builder.translation("inbox.config.layout.pos.inbox_btn").push("inbox_buttons");

					this.Chat_Gui_Button = new Pos(builder, "chat_gui_button", "inbox.config.layout.pos.inbox_btn.chat_btn", 25, 5);
					this.Survival_Gui_Button = new Pos(builder, "survival_gui_button", "inbox.config.layout.pos.inbox_btn.survival_btn", 76, 49);
					this.Creative_Tab_Button = new Pos(builder, "creative_tab_button", "inbox.config.layout.pos.inbox_btn.creative_btn", 170, 165);

					this.Chat_Gui_Button_Size = builder
							.translation("inbox.config.layout.pos.inbox_btn.chat_btn.size")
							.comment("inbox.config.layout.pos.inbox_btn.chat_btn.size.0")
							.defineInRange("chat_gui_button_size", 1d, 0.15d, 10d);
					this.Survival_Gui_Button_Size = builder
							.translation("inbox.config.layout.pos.inbox_btn.survival_btn.size")
							.comment("inbox.config.layout.pos.inbox_btn.survival_btn.size.0")
							.defineInRange("survival_gui_button_size", 0.8d, 0.15d, 10d);
					this.Creative_Tab_Button_Size = builder
							.translation("inbox.config.layout.pos.inbox_btn.creative_btn.size")
							.comment("inbox.config.layout.pos.inbox_btn.creative_btn.size.0")
							.defineInRange("creative_tab_button_size", 1.05d, 0.15d, 10d);

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
		public final Time cooling;
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

			this.cooling = new Time(builder, "inbox.config.send.cooling", "cooling", 0, 0, 0, 5, 0, 0);

			this.Send_History_Max_Count = builder
					.translation("inbox.config.send.history_max")
					.comment("send history max count")
					.defineInRange("Send_History_Max_Count", 5, 0, Integer.MAX_VALUE);

			builder.pop();
		}
	}

	public static class SQLProperties extends BaseConfig {
		public final EnumValue<SQLDatabaseDriver> Database_Driver;
		public final ConfigValue<String> Database_Url;
		public final ConfigValue<String> Database_Username;
		public final ConfigValue<String> Database_Password;

		public SQLProperties(Builder builder) {
			super(builder);
			builder.translation("inbox.config.sql_properties").push("sql_properties");

			this.Database_Driver = builder
					.translation("inbox.config.sql_properties.driver")
					.defineEnum("sql_driver", SQLDatabaseDriver.SQLite);

			this.Database_Url = builder
					.translation("inbox.config.sql_properties.url")
					.comment("{root} = inbox root save path. default is '/<minecraft>/saves/<world>/'")
					.define("sql_url", "{root}/inbox.db");

			this.Database_Username = builder
					.translation("inbox.config.sql_properties.user")
					.define("sql_username", "");

			this.Database_Password = builder
					.translation("inbox.config.sql_properties.pwd")
					.define("sql_password", "");

			builder.pop();
		}
	}

	public static final ForgeConfigSpec CONFIG_MAIN;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		Save_To_Minecraft_Root_Directory = builder
				.worldRestart()
				.translation("inbox.config.save_to_root_directory")
				.comment("inbox.config.save_to_root_directory.0",
						"inbox.config.save_to_root_directory.1")
				.define("Save_To_Minecraft_Root_Directory", false);

		Storage_Inbox_Types = builder
				.translation("inbox.config.storage_type")
				.comment(
						"inbox.config.storage_type.0",
						"inbox.config.storage_type.1",
						"inbox.config.storage_type.2",
						"inbox.config.storage_type.3",
						"Allowed Values: " + String.join(", ", StorageType.REGISTRY.getIDs())
				)
				.define("storage_types", "json"::toString, k->StorageType.REGISTRY.registered(String.valueOf(k)));

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
		SQL_PROPERTIES = new SQLProperties(builder);

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
	public static class Time extends BaseConfig {
		public final IntValue Day;
		public final IntValue Hour;
		public final IntValue Minute;
		public final IntValue Second;
		public final IntValue Tick;
		public final IntValue Millis;
		public Time(Builder builder, String translationKey, String path, int defaultDay, int defaultHour, int defaultMinute, int defaultSecond, int defaultTick, int defaultMillis) {
			super(builder);
			builder.translation(translationKey).push(path);

			this.Day = builder
					.translation("inbox.config.time.day")
					.defineInRange("Day", defaultDay, 0, Integer.MAX_VALUE);

			this.Hour = builder
					.translation("inbox.config.time.hour")
					.defineInRange("Hour", defaultHour, 0, Integer.MAX_VALUE);

			this.Minute = builder
					.translation("inbox.config.time.minute")
					.defineInRange("Minute", defaultMinute, 0, Integer.MAX_VALUE);

			this.Second = builder
					.translation("inbox.config.time.second")
					.defineInRange("Second", defaultSecond, 0, Integer.MAX_VALUE);

			this.Tick = builder
					.translation("inbox.config.time.tick")
					.defineInRange("Tick", defaultTick, 0, Integer.MAX_VALUE);

			this.Millis = builder
					.translation("inbox.config.time.millis")
					.defineInRange("Millis", defaultMillis, 0, Integer.MAX_VALUE);

			builder.pop();
		}

		public long getTicks() {
			return EmailUtils.parseTick(this.Day.get(), this.Hour.get(), this.Minute.get(), this.Second.get(), this.Tick.get()) + this.Millis.get() / 50;
		}
		public long getMillis() {
			return EmailUtils.parseMillis(this.Day.get(), this.Hour.get(), this.Minute.get(), this.Second.get(), this.Tick.get(), this.Millis.get());
		}
	}

	@Deprecated
	public static boolean isInfiniteSize() {
		return false;
//		return EmailMain.proxy.isClient()
//			&& Minecraft.getInstance().isIntegratedServerRunning()
//			&& EmailConfigs.Enable_Inbox_Infinite_Storage_Cache.get();
	}
}
