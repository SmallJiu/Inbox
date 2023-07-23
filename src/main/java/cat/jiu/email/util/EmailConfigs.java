package cat.jiu.email.util;

import cat.jiu.email.EmailMain;

import net.minecraft.client.Minecraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;

public final class EmailConfigs {
	public static final BooleanValue Enable_Inbox_Infinite_Storage_Cache;
	public static final BooleanValue Save_To_Minecraft_Root_Directory;
	public static final BooleanValue Save_Inbox_To_SQL;
	public static final Main Main;
	public static class Main extends BaseConfig {
		public final IntValue Selected_Text_Rows;
		public final IntValue Selected_Text_Spacing;
		public final BooleanValue Enable_Vanilla_Wrap_Text;
		public final Size Size;
		public final Position Position;
		public NumberOfWords Number_Of_Words;

		public Main(ForgeConfigSpec.Builder builder) {
			super(builder);
			builder.comment("Main settings").push("main");
			this.Selected_Text_Rows = builder
					.translation("email.config.main.show_text_rows")
					.comment("selected text rows")
					.defineInRange("Selected_Text_Rows", 6, 1, 8);
			this.Selected_Text_Spacing = builder
					.translation("email.config.main.show_text_spacing")
					.comment("selected text spacing")
					.defineInRange("Selected_Text_Spacing", 3, 0, Integer.MAX_VALUE);
			this.Enable_Vanilla_Wrap_Text = builder
					.translation("email.config.main.vanilla_wrap")
					.comment("use vanilla to wrap text if true, else will use Single char wrap.")
					.define("Enable_Vanilla_Wrap_Text", true);
			this.Size = new Size(builder);
			this.Position = new Position(builder);
			this.Number_Of_Words = new NumberOfWords(builder);
			builder.pop();
		}

		public static class Size extends BaseConfig {
			public final IntValue Width;
			public final IntValue Height;
			public Size(ForgeConfigSpec.Builder builder) {
				super(builder);
				builder.comment("inbox gui size").push("size");

				this.Width = builder.defineInRange("Width", 236, 1, Integer.MAX_VALUE);
				this.Height = builder.defineInRange("Height", 168, 1, Integer.MAX_VALUE);

				builder.pop();
			}
		}

		public static class Position extends BaseConfig {
			public final CurrentEmail Current_Email;
			public final Pos Candidate_Email;
			public Position(ForgeConfigSpec.Builder builder) {
				super(builder);
				builder.comment("text position").push("position");

				this.Current_Email = new CurrentEmail(builder);
				this.Candidate_Email = new Pos(builder, "Candidate_Email", 18, 11, "current email position");

				builder.pop();
			}

			public static class CurrentEmail extends BaseConfig {
				public final Pos Row;
				public final Pos Msg;
				public final Pos Sender;
				public final Pos MsgID;
				public final Pos Items;
				public final Pos Title;
				public final Pos Time;
				public CurrentEmail(ForgeConfigSpec.Builder builder) {
					super(builder);
					builder.comment("current email position").push("current_email");

					this.Row = new Pos(builder, "Row", 93, 33, "row position");
					this.Msg = new Pos(builder, "Msg", 101, 33, "message position");
					this.Sender = new Pos(builder, "Sender", 88, 20, "sender position");
					this.MsgID = new Pos(builder, "MsgID", 80, 6, "current msg ID position");
					this.Items = new Pos(builder, "Items", 48, 109, "items position");
					this.Title = new Pos(builder, "Title", 88, 6, "title position");
					this.Time = new Pos(builder, "Time", 161, 20, "send time position");

					builder.pop();
				}
			}
		}
		public static class NumberOfWords extends BaseConfig {
			public final CurrentEmail Current_Email;
			public final CandidateEmail Candidate_Email;
			public NumberOfWords(ForgeConfigSpec.Builder builder) {
				super(builder);
				builder.comment("Number of words").push("number_of_words");
				this.Current_Email = new CurrentEmail(builder);
				this.Candidate_Email = new CandidateEmail(builder);
				builder.pop();
			}

			public static class CurrentEmail extends BaseConfig {
				public final IntValue Message;
				public final IntValue Title;
				public final IntValue Sender;
				public CurrentEmail(ForgeConfigSpec.Builder builder) {
					super(builder);
					builder.comment("current message").push("current_email");
					this.Message = builder.defineInRange("Message", 106, 1, Integer.MAX_VALUE);
					this.Title = builder.defineInRange("Title", 125, 1, Integer.MAX_VALUE);
					this.Sender = builder.defineInRange("Sender", 61, 1, Integer.MAX_VALUE);
					builder.pop();
				}
			}
			public static class CandidateEmail extends BaseConfig {
				public final IntValue Sender;
				public CandidateEmail(ForgeConfigSpec.Builder builder) {
					super(builder);
					builder.comment("candidate msgs").push("candidate_email");
					this.Sender = builder.defineInRange("Sender", 44, 1, Integer.MAX_VALUE);;
					builder.pop();
				}
			}
		}
	}

	public static Send Send;
	public static class Send extends BaseConfig {
		public final BooleanValue Enable_Send_BlackList;
		public final BooleanValue Enable_Send_WhiteList;
		public final BooleanValue Enable_Send_To_Self;
		public final BooleanValue Enable_Send_Cooling;
		public final BooleanValue Enable_Inbox_Button;
		public final Cooling cooling;

		public Send(ForgeConfigSpec.Builder builder) {
			super(builder);
			builder.comment("email send").push("send");

			this.Enable_Send_BlackList = builder
					.translation("email.config.send.blacklist")
					.comment("enable send email black list,",
							"§cCannot enable at the same time as White List")
					.define("Enable_Send_BlackList", false);

			this.Enable_Send_WhiteList = builder
					.translation("email.config.send.whitelist")
					.comment("enable send email white list,",
							"§cCannot enable at the same time as Black List")
					.define("Enable_Send_WhiteList", false);

			this.Enable_Send_To_Self = builder
					.translation("email.config.send.send_to_self")
					.comment("enable send email to self")
					.define("Enable_Send_To_Self", false);

			this.Enable_Send_Cooling = builder
					.translation("email.config.send.cooling")
					.comment("enable send email need cooling")
					.define("Enable_Send_Cooling", true);

			this.Enable_Inbox_Button = builder
					.translation("email.config.send.inbox_button")
					.comment("enable send email gui inbox button")
					.define("Enable_Inbox_Button", false);

			this.cooling = new Cooling(builder);

			builder.pop();
		}

		public static class Cooling extends BaseConfig {
			public IntValue Day;
			public IntValue Hour;
			public IntValue Minute;
			public IntValue Second;
			public IntValue Tick;
			public IntValue Millis;
			public Cooling(ForgeConfigSpec.Builder builder) {
				super(builder);
				builder.comment("send email cooling time").push("cooling");

				this.Day = builder
						.translation("email.config.time.day")
						.comment("send cooling of day")
						.defineInRange("Day", 0, 0, Integer.MAX_VALUE);

				this.Hour = builder
						.translation("email.config.time.hour")
						.comment("send cooling of hour")
						.defineInRange("Hour", 0, 0, Integer.MAX_VALUE);

				this.Minute = builder
						.translation("email.config.time.minute")
						.comment("send cooling of minute")
						.defineInRange("Minute", 0, 0, Integer.MAX_VALUE);

				this.Second = builder
						.translation("email.config.time.second")
						.comment("send")
						.defineInRange("Second", 5, 0, Integer.MAX_VALUE);

				this.Tick = builder
						.translation("email.config.time.tick")
						.comment("send cooling of tick")
						.defineInRange("Tick", 0, 0, Integer.MAX_VALUE);

				this.Millis = builder
						.translation("email.config.time.millis")
						.comment("send cooling of millis")
						.defineInRange("Millis", 0, 0, Integer.MAX_VALUE);

				builder.pop();
			}
		}
	}

	public static final ForgeConfigSpec CONFIG_MAIN;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("General settings").push("general");

		Enable_Inbox_Infinite_Storage_Cache = builder
				.translation("email.config.infinite_size")
				.comment("enable inbox infinite storage cache,",
						"§cWarning: only enable on Single player.")
				.define("Enable_Inbox_Infinite_Storage_Cache", false);

		Save_To_Minecraft_Root_Directory = builder
				.translation("email.config.save_to_root_directory")
				.comment("save email to minecraft root directory",
						"save email to world directory if false",
						"can cross you saves send email if true")
				.worldRestart()
				.define("Save_To_Minecraft_Root_Directory", false);

		Save_Inbox_To_SQL = builder
				.translation("email.config.save_inbox_to_sql")
				.comment("save inbox to sql file(inboxs.db)",
						"§cWarning: you need backup old inbox files!")
				.worldRestart()
				.define("Save_Inbox_To_SQL", false);

		Main = new Main(builder);
		Send = new Send(builder);

		builder.pop();
		CONFIG_MAIN = builder.build();
	}

	public static class Pos extends BaseConfig {
		public final IntValue X;
		public final IntValue Y;
		public Pos(Builder builder, String pathName, int x, int y, String... comments) {
			super(builder);
			builder.comment(comments).push(pathName);
			this.X = builder.defineInRange("X", x, Integer.MIN_VALUE, Integer.MAX_VALUE);
			this.Y = builder.defineInRange("Y", y, Integer.MIN_VALUE, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class BaseConfig {
		public BaseConfig(@SuppressWarnings("unused") ForgeConfigSpec.Builder builder) {}
	}
	
	public static boolean isInfiniteSize() {
		return EmailMain.proxy.isClient()
			&& Minecraft.getInstance().isIntegratedServerRunning()
			&& EmailConfigs.Enable_Inbox_Infinite_Storage_Cache.get();
	}
}
