package cat.jiu.email.ui.container;

import cat.jiu.email.ui.GuiHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ContainerScheduledEmail extends AbstractContainerMenu {
    public ContainerScheduledEmail(int guiID) {
        super(GuiHandler.scheduled_TYPE.get(), guiID);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}
