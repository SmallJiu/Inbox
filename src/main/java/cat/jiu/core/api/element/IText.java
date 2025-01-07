package cat.jiu.core.api.element;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.element.Text;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
//		TextFormatEvent event = new TextFormatEvent(this.getText(), this.getParameters());
//		if(MinecraftForge.EVENT_BUS.post(event) && event.getFormatResult() != null) {
//			return event.getFormatResult();
//		}
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
	default void read(JsonObject data) {
		this.setText(JsonUtils.get(data, "text", JsonUtils.get(data, "key", "empty")));
		this.setUseVanillaWrap(JsonUtils.get(data, "isVanillaWrap", false));
		this.setCenter(JsonUtils.get(data, "isCenter", false));

		if(data.has("parameters") || data.has("args")) {
			JsonArray parametersArray = data.getAsJsonArray(data.has("parameters") ? "parameters" : "args");
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
	default JsonObject write(JsonObject data) {
		if(data == null)
			data = new JsonObject();
		
		data.addProperty("text", this.getText());
		data.addProperty("isCenter", this.isCenter());
		data.addProperty("isVanillaWrap", this.isVanillaWrap());

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
			data.add("parameters", parametersArray);
		}
		
		return data;
	}

	@Override
	default void read(CompoundTag data) {
		this.setText(NBTUtils.get(data, "text", NBTUtils.get(data, "key", "empty")));
		this.setUseVanillaWrap(NBTUtils.get(data, "isVanillaWrap", false));
		this.setCenter(NBTUtils.get(data, "isCenter", false));

		if(data.contains("parameters")) {
			CompoundTag parametersArray = data.getCompound("parameters");
			Object[] parameters = new Object[parametersArray.size()];
			List<String> keys = parametersArray.getAllKeys().stream().sorted(Comparator.comparingLong(Long::valueOf)).toList();

			for(int i = 0; i < keys.size(); i++) {
				Tag e = parametersArray.get(keys.get(i));
				if(e instanceof CompoundTag) {
					parameters[i] = new Text((CompoundTag)e);
				}else {
					parameters[i] = e.getAsString();
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
