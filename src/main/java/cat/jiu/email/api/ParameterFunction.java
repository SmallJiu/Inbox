package cat.jiu.email.api;

import net.minecraft.entity.player.PlayerEntity;

@FunctionalInterface
public interface ParameterFunction {
    String parser(String key, String cmd, PlayerEntity player);
}
