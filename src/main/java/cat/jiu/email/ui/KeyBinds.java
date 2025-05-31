package cat.jiu.email.ui;

import cat.jiu.core.util.client.KeyUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBinds {
    public static final KeyUtil KEY_DELETE_EMAIL = KeyUtil.of(
            "info.inbox.key.delete",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            "info.inbox.name"
    );
    public static final KeyUtil KEY_ACCEPT_EMAIL = KeyUtil.of(
            "info.inbox.key.accept",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_ENTER,
            "info.inbox.name"
    );
    public static final KeyUtil KEY_BUTTON_DRAGGING = KeyUtil.of(
            "info.inbox.key.dragging",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "info.inbox.name"
    );
}
