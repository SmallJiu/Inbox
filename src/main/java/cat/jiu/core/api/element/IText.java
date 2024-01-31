package cat.jiu.core.api.element;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.events.client.TextFormatEvent;
import cat.jiu.core.util.element.Text;
import crafttweaker.annotations.ZenRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("core.Text")
public interface IText extends ISerializable {
	@ZenGetter("text")
	String getText();
	@ZenMethod("text")
	IText setText(String text);
	
	@ZenGetter("args")
	Object[] getParameters();
	@ZenMethod("args")
	IText setParameters(Object... parameters);

	@ZenGetter("center")
	boolean isCenter();
	@ZenMethod("center")
	IText setCenter(boolean isCenter);
	
	/**
	 * 使用原版的长行回绕，false则无视语序单字回绕<p>
	 * use {@link FontRenderer#listFormattedStringToWidth(String, int)} to format text if true, <p>
	 * else use {@link cat.jiu.dialog.ui.GuiDialog#splitString(String, int)} to format text.
	 * @see net.minecraft.client.gui.FontRenderer#listFormattedStringToWidth(String, int)
	 * @see cat.jiu.dialog.ui.GuiDialog#splitString(String, int)
	 */
	@ZenGetter("vanWrap")
	boolean isVanillaWrap();
	@ZenMethod("vanWrap")
	IText setUseVanillaWrap(boolean isVanillaWrap);
	
	@ZenMethod
	IText copy();
	
	@ZenMethod
	@SideOnly(Side.CLIENT)
	default String format() {
		TextFormatEvent event = new TextFormatEvent(this.getText(), this.getParameters());
		if(MinecraftForge.EVENT_BUS.post(event) && event.getFormatResult() != null) {
			return event.getFormatResult();
		}
		return I18n.format(this.getText(), IText.format(this.getParameters()));
	}

	@SideOnly(Side.CLIENT)
	static Object[] format(Object... args) {
		Object[] arg = Arrays.copyOf(args, args.length);
		for(int i = 0; i < arg.length; i++) {
			Object object = arg[i];
			if(object instanceof IText) {
				arg[i] = ((IText) object).format();
			}
		}
		return arg;
	}

	@Deprecated
	@SideOnly(Side.CLIENT)
	default int getStringWidth(FontRenderer fr) {
		return fr.getStringWidth(this.format());
	}
	@ZenMethod("width")
	@SideOnly(Side.CLIENT)
	default int getStringWidth() {
		return Minecraft.getMinecraft().fontRenderer.getStringWidth(this.format());
	}

	default TextComponentTranslation toTextComponent() {
		Object[] objs = this.getParameters();
		for(int i = 0; i < objs.length; i++) {
			if(objs[i] instanceof IText) {
				objs[i] = ((IText)objs[i]).toTextComponent();
			}
		}
		return new TextComponentTranslation(this.getText(), objs);
	}

	@ZenMethod
	@Override
	default void read(JsonObject json) {
		this.setText(json.get("text").getAsString());
		if(json.has("isVanillaWrap")) this.setUseVanillaWrap(json.get("wrap").getAsBoolean());
		if(json.has("isCenter")) this.setCenter(json.get("isCenter").getAsBoolean());
		if(json.has("parameters")) {
			JsonArray parametersArray = json.getAsJsonArray("parameters");
			Object[] parameters = new Object[parametersArray.size()];
			for(int i = 0; i < parameters.length; i++) {
				JsonElement e = parametersArray.get(i);
				if(e.isJsonObject()) {
					parameters[i] = new Text(e.getAsJsonObject());
				}else if(e.isJsonPrimitive()) {
					parameters[i] = e.getAsString();
				}
			}
			this.setParameters(parameters);
		}
	}

	@ZenMethod
	@Override
	default JsonObject write(JsonObject json) {
		if(json == null)
			json = new JsonObject();
		
		json.addProperty("text", this.getText());
		if(this.isCenter()) json.addProperty("isCenter", this.isCenter());
		if(this.isVanillaWrap()) json.addProperty("isVanillaWrap", this.isVanillaWrap());
		if(this.getParameters()!=null && this.getParameters().length > 0) {
			JsonArray parametersArray = new JsonArray();
			for(int i = 0; i < this.getParameters().length; i++) {
				Object o = this.getParameters()[i];
				if(o instanceof IText) {
					parametersArray.add(((IText) o).writeTo(JsonObject.class));
				}else {
					parametersArray.add(String.valueOf(o));
				}
			}
			json.add("parameters", parametersArray);
		}
		
		return json;
	}

	@ZenMethod
	@Override
	default void read(NBTTagCompound nbt) {
		this.setText(nbt.getString("text"));
		if(nbt.hasKey("isVanillaWrap")) this.setUseVanillaWrap(nbt.getBoolean("wrap"));
		if(nbt.hasKey("isCenter")) this.setCenter(nbt.getBoolean("isCenter"));
		if(nbt.hasKey("parameters")) {
			NBTTagCompound parametersArray = nbt.getCompoundTag("parameters");
			Object[] parameters = new Object[parametersArray.getSize()];
			List<String> keys = parametersArray.getKeySet().stream().sorted((k1, k2)-> Long.compare(Long.valueOf(k1), Long.valueOf(k2))).collect(Collectors.toList());
			
			for(int i = 0; i < keys.size(); i++) {
				NBTBase e = parametersArray.getTag(keys.get(i));
				if(e instanceof NBTTagCompound) {
					parameters[i] = new Text((NBTTagCompound)e);
				}else {
					parameters[i] = e.toString();
				}
			}
			this.setParameters(parameters);
		}
	}

	@ZenMethod
	@Override
	default NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt == null)
			nbt = new NBTTagCompound();
		
		nbt.setString("text", this.getText());
		if(this.isCenter()) nbt.setBoolean("isCenter", this.isCenter());
		if(this.isVanillaWrap()) nbt.setBoolean("isVanillaWrap", this.isVanillaWrap());
		if(this.getParameters()!=null && this.getParameters().length > 0) {
			NBTTagCompound parametersTag = new NBTTagCompound();
			for(int i = 0; i < this.getParameters().length; i++) {
				Object o = this.getParameters()[i];
				if(o instanceof IText) {
					parametersTag.setTag(String.valueOf(i), ((IText) o).writeTo(NBTTagCompound.class));
				}else {
					parametersTag.setString(String.valueOf(i), String.valueOf(o));
				}
			}
			nbt.setTag("parameters", parametersTag);
		}
		
		return nbt;
	}
	
	@ZenMethod
	static IText from(String key) {
		return new Text(key);
	}
	@ZenMethod
	static IText from(String key, Object... args) {
		return new Text(key, args);
	}
}
