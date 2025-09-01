package cat.jiu.email.configs;

import cat.jiu.core.util.base.BaseConfig;
import cat.jiu.email.util.RenderCorner;
import net.minecraftforge.common.ForgeConfigSpec;

public class EmailConfigClient {
    public static final ForgeConfigSpec CONFIG_MAIN;

    public static final ForgeConfigSpec.BooleanValue Enable_Chat_Button;
    public static final ForgeConfigSpec.IntValue Email_List_Width;
    public static final ForgeConfigSpec.BooleanValue Enable_Vanilla_Wrap_Text;
    public static final ForgeConfigSpec.BooleanValue Lock_Inbox_Button_Dragging;
    public static final Position Position;
    public static final EmailConfigServer.Time Prompt_Email;
    public static final ForgeConfigSpec.IntValue Send_History_Max_Count;
    public static final ForgeConfigSpec.EnumValue<RenderCorner> Undying_Count_Render_Side;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        Enable_Vanilla_Wrap_Text = builder
                .translation("inbox.config.layout.vanilla_wrap")
                .comment("inbox.config.layout.vanilla_wrap.0",
                        "inbox.config.layout.vanilla_wrap.1")
                .define("Enable_Vanilla_Wrap_Text", true);

        Lock_Inbox_Button_Dragging = builder
                .translation("inbox.config.layout.button_dragging")
                .comment("inbox.config.layout.button_dragging.0")
                .define("Lock_Inbox_Button_Dragging", false);

        Enable_Chat_Button = builder
                .translation("inbox.config.enable_chat_btn")
                .comment("inbox.config.enable_chat_btn.0")
                .define("Enable_Chat_Button", false);

        Email_List_Width = builder
                .translation("inbox.config.layout.email_list_width")
                .comment("inbox.config.layout.email_list_width.0")
                .defineInRange("Email_List_Width", 100, 0, Integer.MAX_VALUE);

        Position = new Position(builder);
        Prompt_Email = new EmailConfigServer.Time(builder, "inbox.config.layout.prompt", "prompt", 0, 0, 0, 25, 0, 0);

        Send_History_Max_Count = builder
                .translation("inbox.config.send.history_max")
                .comment("send history max count")
                .defineInRange("Send_History_Max_Count", 5, 0, Integer.MAX_VALUE);

        Undying_Count_Render_Side = builder
                .translation("inbox.config.layout.corner")
                .comment("inbox.config.layout.corner.0")
                .defineEnum("Undying_Count_Render_Side", RenderCorner.lower_left);

        CONFIG_MAIN = builder.build();
    }

    public static class Position extends BaseConfig {
        public final InboxButtons Inbox_Buttons;
        Position(ForgeConfigSpec.Builder builder) {
            super(builder);
            builder.translation("inbox.config.layout.pos").push("position");

            this.Inbox_Buttons = new InboxButtons(builder);

            builder.pop();
        }

        public static class InboxButtons extends BaseConfig {
            public final EmailConfigServer.Pos Chat_Gui_Button;
            public final EmailConfigServer.Pos Survival_Gui_Button;
            public final EmailConfigServer.Pos Creative_Tab_Button;
            public final ForgeConfigSpec.DoubleValue Chat_Gui_Button_Size;
            public final ForgeConfigSpec.DoubleValue Survival_Gui_Button_Size;
            public final ForgeConfigSpec.DoubleValue Creative_Tab_Button_Size;

            public InboxButtons(ForgeConfigSpec.Builder builder) {
                super(builder);
                builder.translation("inbox.config.layout.pos.inbox_btn").push("inbox_buttons");

                this.Chat_Gui_Button = new EmailConfigServer.Pos(builder, "chat_gui_button", "inbox.config.layout.pos.inbox_btn.chat_btn", 25, 5);
                this.Survival_Gui_Button = new EmailConfigServer.Pos(builder, "survival_gui_button", "inbox.config.layout.pos.inbox_btn.survival_btn", 76, 49);
                this.Creative_Tab_Button = new EmailConfigServer.Pos(builder, "creative_tab_button", "inbox.config.layout.pos.inbox_btn.creative_btn", 170, 165);

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

    static {

    }
}
