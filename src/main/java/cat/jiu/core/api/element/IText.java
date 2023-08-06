package cat.jiu.core.api.element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.events.client.TextFormatEvent;
import cat.jiu.core.util.element.Text;
import cat.jiu.sql.SQLValues;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

public interface IText extends ISerializable {
	String getText();
	IText setText(String text);

	Object[] getParameters();
	IText setParameters(Object... parameters);
	
	boolean isCenter();
	IText setCenter(boolean isCenter);

	boolean isVanillaWrap();
	IText setUseVanillaWrap(boolean isVanillaWrap);
	
	IText copy();

	@OnlyIn(Dist.CLIENT)
	default String format() {
		TextFormatEvent event = new TextFormatEvent(this.getText(), this.getParameters());
		if(MinecraftForge.EVENT_BUS.post(event) && event.getFormatResult() != null) {
			return event.getFormatResult();
		}
		return I18n.get(this.getText(), IText.format(this.getParameters()));
	}

	@OnlyIn(Dist.CLIENT)
	static Object[] format(Object... args) {
		Object[] arg = Arrays.copyOf(args, args.length);
		for(int i = 0; i < arg.length; i++) {
			Object object = arg[i];
			if(object instanceof IText) {
				arg[i] = ((IText) object).format();
			}else if(object instanceof Component){
				arg[i] = ((Component) object).getString();
			}
		}
		return arg;
	}

	@OnlyIn(Dist.CLIENT)
	default int getStringWidth(Font fr) {
		return fr.width(this.format());
	}

	default Component toTextComponent() {
		return Component.translatable(this.getText(), this.getParameters());
	}
	default Component toTextComponent(ChatFormatting color) {
		Component text = Component.translatable(this.getText(), this.getParameters());
		return text.copy().setStyle(text.getStyle().withColor(color));
	}
	default Component toTextComponent(TextColor color) {
		Component text = Component.translatable(this.getText(), this.getParameters());
		return text.copy().setStyle(text.getStyle().withColor(color));
	}

	@Override
	default void read(JsonObject json) {
		if(json.has("text")) {
			this.setText(json.get("text").getAsString());
		}else if(json.has("key")) {
			this.setText(json.get("key").getAsString());
		}
		
		if(json.has("isVanillaWrap")) this.setUseVanillaWrap(json.get("isVanillaWrap").getAsBoolean());
		if(json.has("isCenter")) this.setCenter(json.get("isCenter").getAsBoolean());
		if(json.has("parameters") || json.has("args")) {
			JsonArray parametersArray = json.has("parameters") ? json.getAsJsonArray("parameters") : json.getAsJsonArray("args");
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

	@Override
	default void read(CompoundTag nbt) {
		this.setText(nbt.getString("text"));
		if(nbt.contains("isVanillaWrap")) this.setUseVanillaWrap(nbt.getBoolean("isVanillaWrap"));
		if(nbt.contains("isCenter")) this.setCenter(nbt.getBoolean("isCenter"));
		if(nbt.contains("parameters")) {
			CompoundTag parametersArray = nbt.getCompound("parameters");
			Object[] parameters = new Object[parametersArray.size()];
			List<String> keys = parametersArray.getAllKeys().stream().sorted(Comparator.comparingLong(Long::valueOf)).collect(Collectors.toList());

			for(int i = 0; i < keys.size(); i++) {
				Tag e = parametersArray.get(keys.get(i));
				if(e instanceof CompoundTag) {
					parameters[i] = new Text((CompoundTag)e);
				}else {
					parameters[i] = e.toString();
				}
			}
			this.setParameters(parameters);
		}
	}

	@Override
	default CompoundTag write(CompoundTag nbt) {
		if(nbt == null)
			nbt = new CompoundTag();
		
		nbt.putString("text", this.getText());
		if(this.isCenter()) nbt.putBoolean("isCenter", this.isCenter());
		if(this.isVanillaWrap()) nbt.putBoolean("isVanillaWrap", this.isVanillaWrap());
		if(this.getParameters()!=null && this.getParameters().length > 0) {
			CompoundTag parametersTag = new CompoundTag();
			for(int i = 0; i < this.getParameters().length; i++) {
				Object o = this.getParameters()[i];
				if(o instanceof IText) {
					parametersTag.put(String.valueOf(i), ((IText) o).writeTo(CompoundTag.class));
				}else {
					parametersTag.putString(String.valueOf(i), String.valueOf(o));
				}
			}
			nbt.put("parameters", parametersTag);
		}
		
		return nbt;
	}
	
	@Override
	default SQLValues write(SQLValues value) {
		return value;
	}
	@Override
	default void read(ResultSet result) throws SQLException {
		
	}
}
