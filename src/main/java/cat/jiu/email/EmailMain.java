package cat.jiu.email;

import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.command.EmailCommands;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.net.msg.MsgUnreceive;
import cat.jiu.email.net.msg.MsgUnread;
import cat.jiu.email.proxy.ServerProxy;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(
	modid = EmailMain.MODID,
	name = EmailMain.NAME,
	version = EmailMain.VERSION,
	useMetadata = true,
	guiFactory = "cat.jiu.email.util.ConfigGuiFactory",
	dependencies = "after:jiucore@[1.1.0-20220608013004,);",
	acceptedMinecraftVersions = "[1.12.2]"
)
@Mod.EventBusSubscriber
public class EmailMain {
	public static final String MODID = "email";
	public static final String NAME = "E-mail";
	public static final String OWNER = "small_jiu";
	public static final String VERSION = "1.0.0-a0-20220919000017";
	public static EmailNetworkHandler net;
	public static final Logger log = LogManager.getLogger("Email");
	public static final String SYSTEM = "?????";
	public static MinecraftServer server;
	
	@SidedProxy(
		serverSide = "cat.jiu.email.proxy.ServerProxy",
		clientSide = "cat.jiu.email.proxy.ClientProxy",
		modId = EmailMain.MODID
	)
	public static ServerProxy proxy;
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		new EmailGuiHandler();
		net = new EmailNetworkHandler();
		
		if(EmailConfigs.Send.Enable_Send_BlackList && EmailConfigs.Send.Enable_Send_WhiteList) {
			EmailConfigs.Send.Enable_Send_BlackList = false;
			EmailConfigs.Send.Enable_Send_WhiteList = false;
		}
	}

	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		if(server == null) {
			server = event.getServer();
		}
		EmailUtils.getSaveEmailPath();
		EmailUtils.initNameAndUUID(event.getServer());
		event.registerServerCommand(new EmailCommands());
	}

	@Mod.EventHandler
	public void onServerClose(FMLServerStoppedEvent event) {
		server = null;
		EmailUtils.resetPath();
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof EntityPlayer && !event.getWorld().isRemote) {
			UUID uid = event.getEntity().getUniqueID(); 
			JsonObject obj = EmailUtils.getEmail(uid.toString());
			if(obj == null) {
				obj = new JsonObject();
			}
			if(!obj.has("dev")) {
				JsonObject email = new JsonObject();
				email.addProperty("dev", true);
				
				JsonObject devEmail = new JsonObject(); {
					devEmail.addProperty("sender", "email.dev_message.sender");
					devEmail.addProperty("time", MsgSend.getTime());
					devEmail.addProperty("title", "email.dev_message.title");
					JsonArray msgs = new JsonArray();
					for(int i = 0; i < 7; i++) {
						msgs.add("email.dev_message."+i);
					}
					devEmail.add("msgs", msgs);
				}
				email.add(Integer.toString(obj.size()), devEmail);
				EmailUtils.toJsonFile(uid.toString(), email);
			}
		}
	}
	
	@SubscribeEvent
	public static void onJoinWorld(EntityJoinWorldEvent event) {
		if(event.getEntity() instanceof EntityPlayer && !event.getWorld().isRemote) {
			JsonObject emailOBJ = EmailUtils.getEmail(event.getEntity().getUniqueID().toString());
			if(emailOBJ != null) {
				int unread = getUn(emailOBJ, "read");
				int unaccept = getUn(emailOBJ, "accept");
				if(unread > 0) {
					if(!proxy.isClient()) {
						net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) event.getEntity());
					}else {
						new Thread(()->{
							// for network delay, need send after
							try {Thread.sleep(100);}catch(InterruptedException e) { e.printStackTrace();}
							EmailMain.setUnread(unread);
						}).start();
					}
				}
				if(unaccept > 0) {
					if(!proxy.isClient()) {
						net.sendMessageToPlayer(new MsgUnreceive(unaccept), (EntityPlayerMP) event.getEntity());
					}else {
						new Thread(()->{
							// for network delay, need send after
							try {Thread.sleep(100);}catch(InterruptedException e) { e.printStackTrace();}
							EmailMain.setAccept(unaccept);
						}).start();
					}
				}
			}
		}
	}
	
	public static int getUn(JsonObject email, String key) {
		int un = 0;
		for(Entry<String, JsonElement> msgs : email.entrySet()) {
			if(msgs.getValue().isJsonObject()) {
				JsonObject msg = msgs.getValue().getAsJsonObject();
				if("accept".equals(key)) {
					if(!msg.has("accept") && msg.has("items")) {
						un++;
						continue;
					}
				}else if(msg.has(key)) {
					continue;
				}else {
					un++;
				}
			}
		}
		return un;
	}
	
	@SubscribeEvent
	public static void onServerTick(TickEvent.PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(!player.world.isRemote) {
			NBTTagCompound entityNBT = player.getEntityData();
			NBTTagCompound emailNBT = null;
			if(entityNBT.hasKey("email")) {
				emailNBT = entityNBT.getCompoundTag("email");
			}else {
				emailNBT = new NBTTagCompound();
			}
			
			int time = 0;
			if(emailNBT.hasKey("unAcceptTime")) {
				time = emailNBT.getInteger("unAcceptTime");
			}
			
			if(time <= 0) {
				time = (int) EmailUtils.parseTick(15, 0);
				JsonObject email = EmailUtils.getEmail(player.getUniqueID().toString());
				if(email != null) {
					int unread = getUn(email, "read");
					int unreceive = getUn(email, "accept");
					if(unread > 0) {
						if(unreceive > 0) {
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread_and_unreceive", unread, unreceive), true);
							net.sendMessageToPlayer(new MsgUnreceive(unreceive), (EntityPlayerMP) player);
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
						}else {
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread", unread), true);
						}
					}else if(unreceive > 0) {
						if(unread > 0) {
							net.sendMessageToPlayer(new MsgUnreceive(unreceive), (EntityPlayerMP) player);
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread_and_unreceive", unread, unreceive), true);
						}else {
							net.sendMessageToPlayer(new MsgUnreceive(unreceive), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unreceive", unreceive), true);
						}
					}
				}
			}else {
				time--;
			}
			emailNBT.setInteger("unAcceptTime", time);
			entityNBT.setTag("email", emailNBT);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
		GuiScreen gui = event.getGui();
		if(gui instanceof net.minecraft.client.gui.GuiChat) {
			event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(), gui.width - 20 - 5, 5, I18n.format("info.email.name")));
			event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_SEND, event.getButtonList().size(), gui.width - 20 - 5, 26, I18n.format("info.email.dispatch")));
		}else if(gui instanceof net.minecraft.client.gui.inventory.GuiContainer) {
			net.minecraft.client.gui.inventory.GuiContainer con = (net.minecraft.client.gui.inventory.GuiContainer) gui;
			
			if(con instanceof net.minecraft.client.gui.inventory.GuiInventory) {
				event.getButtonList().add(new InventoryButton(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(),con.getGuiLeft()+27, con.getGuiTop() + 9, I18n.format("info.email.name")));
			}else if(con instanceof net.minecraft.client.gui.inventory.GuiContainerCreative) {
				event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(), con.getGuiLeft() + 145, con.getGuiTop() + 138, I18n.format("info.email.name")));
			}
		}
	}
	
	static final ResourceLocation send_email = new ResourceLocation(MODID, "textures/gui/send_email.png");
	static final ResourceLocation send_email_hover = new ResourceLocation(MODID, "textures/gui/send_email_hover.png");
	static final ResourceLocation inbox = new ResourceLocation(MODID, "textures/gui/inbox_min.png");
	static final ResourceLocation inbox_hover = new ResourceLocation(MODID, "textures/gui/inbox_min_hover.png");
	static final ResourceLocation email = new ResourceLocation(MODID, "textures/gui/email.png");
	
	@SideOnly(Side.CLIENT)
	private static class Button extends GuiButton {
		private final int guiId;
		private final GuiScreen gui;
		public Button(GuiScreen gui, int guiId, int buttonId, int x, int y, String buttonText) {
			super(buttonId, x, y, 20, 13, buttonText);
			this.guiId = guiId;
			this.gui = gui;
		}
		
		int tick = 0;
		int i = 10;
		boolean lag = false;
		
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
				this.hovered = true;
			}else {
				this.hovered = false;
			}
			
			if(this.guiId == EmailGuiHandler.EMAIL_SEND) {
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
				mc.getTextureManager().bindTexture(this.hovered ? send_email : inbox);
			}else if(this.guiId == EmailGuiHandler.EMAIL_MAIN) {
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
			}
			
			drawModalRectWithCustomSizedTexture(this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);
			if((unread > 0 || unaccept > 0) && this.guiId == EmailGuiHandler.EMAIL_MAIN) {
				mc.getTextureManager().bindTexture(email);
				if(this.gui instanceof net.minecraft.client.gui.GuiChat) {
					super.drawTexturedModalRect(this.x - 25 + this.i - 1, this.y + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof net.minecraft.client.gui.inventory.GuiContainerCreative) {
					super.drawTexturedModalRect(this.x + 7, this.y + 15 + this.i - 1, 88, 0, 6, 14);
				}
				if(this.i <= 0) this.lag = true;
				if(this.i >= 8) this.lag = false;
				if(this.tick >= 5) {
					if(this.lag) this.i++;
					else this.i--;
				}
				if(this.tick >= 5) this.tick = 0;
				this.tick++;
			}
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
				this.gui.drawHoveringText(super.displayString, mouseX, mouseY);
			}
		}
		public void mouseReleased(int mouseX, int mouseY) {
			net.sendMessageToServer(new MsgOpenGui(this.guiId));
		}
	}
	
	private static int unread = 0;
	public static void setUnread(int unread) {
		EmailMain.unread = unread;
	}
	
	private static int unaccept = 0;
	public static void setAccept(int unaccept) {
		EmailMain.unaccept = unaccept;
	}
	
	@SideOnly(Side.CLIENT)
	private static class InventoryButton extends GuiButton {
		private final GuiScreen gui;
		private final int guiId;
		private boolean recipeBookOpen;
		public InventoryButton(GuiScreen gui, int guiId, int buttonId, int x, int y, String buttonText) {
			super(buttonId, x, y, 9, 6, buttonText);
			this.guiId = guiId;
			this.gui = gui;
			if(this.gui instanceof GuiInventory && ((GuiInventory) this.gui).func_194310_f().isVisible()) {
				this.recipeBookOpen = true;
			}
		}
		
		int tick = 0;
		int i = 10;
		boolean lag = false;
		
		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if(this.visible) {
				int x = this.x;
				if(this.gui instanceof GuiInventory) {
					GuiInventory inv = (GuiInventory) this.gui;
					if(inv.func_194310_f().isVisible()) {
						x = inv.getGuiLeft() + 27;
					}else {
						if(this.recipeBookOpen) {
							x = this.x - 77;
						}else {
							x = this.x;
						}
					}
				}
				
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1F);
				if(EmailGuiHandler.isInRange(mouseX, mouseY, x, this.y, this.width, this.height)) {
					this.hovered = true;
				}else {
					this.hovered = false;
				}
				
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
				drawModalRectWithCustomSizedTexture(x, this.y, 0, 0, this.width, this.height, this.width, this.height);
				if(unread > 0 || unaccept > 0) {
					mc.getTextureManager().bindTexture(email);
					super.drawTexturedModalRect(x + 9 + this.i + 1, this.y, 73, 15, 15, 6);
					if(this.i <= 0) this.lag = true;
					if(this.i >= 8) this.lag = false;
					if(this.tick >= 5) {
						if(this.lag) this.i++;
						else this.i--;
					}
					if(this.tick >= 5) this.tick = 0;
					this.tick++;
				}
				if(this.hovered) {
					this.gui.drawHoveringText(super.displayString, mouseX, mouseY);
				}
				this.mouseDragged(mc, mouseX, mouseY);
			}
		}
		
		public void mouseReleased(int mouseX, int mouseY) {
			net.sendMessageToServer(new MsgOpenGui(this.guiId));
		}
		
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			int x = this.x;
			if(this.gui instanceof GuiInventory) {
				GuiInventory inv = (GuiInventory) this.gui;
				if(inv.func_194310_f().isVisible()) {
					x = inv.getGuiLeft() + 27;
				}else {
					if(this.recipeBookOpen) {
						x = this.x - 77;
					}else {
						x = this.x;
					}
				}
			}
			return this.enabled && this.visible && EmailGuiHandler.isInRange(mouseX, mouseY, x, this.y, this.width, this.height);
		}
	}
}
