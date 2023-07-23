package cat.jiu.email.init;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.container.*;

import com.google.common.collect.Lists;
import net.minecraft.inventory.container.ContainerType;

import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ContainerTypes {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, EmailMain.MODID);
    public static final RegistryObject<ContainerType<ContainerEmailMain>> container_email_main = CONTAINERS.register("email_main_container", () -> IForgeContainerType.create((windowId, inv, data) -> new ContainerEmailMain(windowId, inv, Inbox.get(inv.player.getUniqueID(), data.readCompoundTag()))));
    public static final RegistryObject<ContainerType<ContainerEmailSend>> container_email_send = CONTAINERS.register("email_send_container", () -> IForgeContainerType.create((windowId, inv, data) -> new ContainerEmailSend(windowId, inv)));
    public static final RegistryObject<ContainerType<ContainerInboxBlacklist>> container_email_blacklist = CONTAINERS.register("email_blacklist_container", () -> IForgeContainerType.create((windowId, inv, data) -> {
        List<String> list = Lists.newArrayList();
        data.readCompoundTag().getList("list", 8).forEach(e->list.add(e.getString()));
        return new ContainerInboxBlacklist(windowId, inv, list);
    }));
}
