package cat.jiu.email.api;

import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface ParameterFunction {
    String parser(String key, String cmd, Player player);
}
