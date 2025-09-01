package cat.jiu.email.util;

import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.function.Supplier;

public enum RenderCorner {
    /** 左上角 */
    upper_left(
            () -> 0,
            () -> 0
    ),
    /** 右上角 */
    upper_right(
            ()->Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            () -> 0
    ),
    /** 左下角 */
    lower_left(
            () -> 0,
            ()->Minecraft.getInstance().getWindow().getGuiScaledHeight()
    ),
    /** 右下角 */
    lower_right(
            ()->Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            ()->Minecraft.getInstance().getWindow().getGuiScaledHeight()
    );

    public final String name;
    public final Supplier<Integer> x, y;
    RenderCorner(Supplier<Integer> x, Supplier<Integer> y) {
        this.x = x;
        this.y = y;
        this.name = "inbox.config.layout.corner." + name().toLowerCase(Locale.ROOT);
    }

    public int getX() {
        return x.get();
    }
    public int getY() {
        return y.get();
    }

    public String getName() {
        return this.name;
    }
}
