package cat.jiu.core.api.element;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.element.Text;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
		return I18n.format(this.getText(), IText.format(this.getParameters()));
	}

	@OnlyIn(Dist.CLIENT)
	static Object[] format(Object... args) {
		Object[] arg = Arrays.copyOf(args, args.length);
		for(int i = 0; i < arg.length; i++) {
			Object object = arg[i];
			if(object instanceof IText) {
				arg[i] = ((IText) object).format();
			}else if(object instanceof ITextComponent){
				arg[i] = ((ITextComponent) object).getString();
			}
		}
		return arg;
	}

	@OnlyIn(Dist.CLIENT)
	default int getStringWidth(FontRenderer fr) {
		return RenderUtils.width(this.format());
	}

	default ITextComponent toTextComponent() {
		return new TranslationTextComponent(this.getText(), this.getParameters());
	}
	default ITextComponent toTextComponent(TextFormatting color) {
		ITextComponent text = new TranslationTextComponent(this.getText(), this.getParameters());
		return text.copyRaw().setStyle(text.getStyle().setFormatting(color));
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
	default void read(CompoundNBT data) {
		this.setText(NBTUtils.get(data, "text", NBTUtils.get(data, "key", "empty")));
		this.setUseVanillaWrap(NBTUtils.get(data, "isVanillaWrap", false));
		this.setCenter(NBTUtils.get(data, "isCenter", false));

		if(data.contains("parameters")) {
			CompoundNBT parametersArray = data.getCompound("parameters");
			Object[] parameters = new Object[parametersArray.size()];
			List<String> keys = parametersArray.keySet().stream().sorted(Comparator.comparingLong(Long::valueOf)).collect(Collectors.toList());

			for(int i = 0; i < keys.size(); i++) {
				INBT e = parametersArray.get(keys.get(i));
				if(e instanceof CompoundNBT) {
					parameters[i] = new Text((CompoundNBT)e);
				}else {
					parameters[i] = e.getString();
				}
			}
			this.setParameters(parameters);
		}
	}

	@Override
	default CompoundNBT write(CompoundNBT nbt) {
		if(nbt == null)
			nbt = new CompoundNBT();
		
		nbt.putString("text", this.getText());
		if(this.isCenter()) nbt.putBoolean("isCenter", this.isCenter());
		if(this.isVanillaWrap()) nbt.putBoolean("isVanillaWrap", this.isVanillaWrap());
		if(this.getParameters()!=null && this.getParameters().length > 0) {
			CompoundNBT parametersTag = new CompoundNBT();
			for(int i = 0; i < this.getParameters().length; i++) {
				Object o = this.getParameters()[i];
				if(o instanceof IText) {
					parametersTag.put(String.valueOf(i), ((IText) o).writeTo(CompoundNBT.class));
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
