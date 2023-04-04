package cat.jiu.email.iface;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.InboxText;
import cat.jiu.email.event.InboxTextFormatEvent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IInboxText {
	String getText();
	void setText(String text);
	
	Object[] getParameters();
	void setParameters(Object... parameters);
	
	@SideOnly(Side.CLIENT)
	default String format() {
		InboxTextFormatEvent event = new InboxTextFormatEvent(this.getText(), this.getParameters());
		if(MinecraftForge.EVENT_BUS.post(event) && event.getFormatResult()!=null) {
			return event.getFormatResult();
		}
		return I18n.format(this.getText(), IInboxText.format(this.getParameters()));
	}
	@SideOnly(Side.CLIENT)
	static Object[] format(Object... args) {
		Object[] arg = Arrays.copyOf(args, args.length);
		for(int i = 0; i < arg.length; i++) {
			Object object = arg[i];
			if(object instanceof IInboxText) {
				arg[i] = ((IInboxText) object).format();
			}
		}
		return arg;
	}
	
	@SideOnly(Side.CLIENT)
	default int getStringWidth(FontRenderer fr) {
		return fr.getStringWidth(this.format());
	}
	default TextComponentTranslation toTextComponent() {
		return new TextComponentTranslation(this.getText(), this.getParameters());
	}
	
	default NBTBase writeToNBT() {
		NBTBase nbt = null;
		
		if(this.getParameters()!=null&&this.getParameters().length>0) {
			nbt=new NBTTagCompound();
			((NBTTagCompound) nbt).setString("key", this.getText());
			NBTTagList args = new NBTTagList();
			for(int i = 0; i < this.getParameters().length; i++) {
				NBTBase tag;
				if(this.getParameters()[i] instanceof IInboxText) {
					tag = ((IInboxText) this.getParameters()[i]).writeToNBT();
				}else {
					tag = new NBTTagString(String.valueOf(this.getParameters()[i]));
				}
				args.appendTag(tag);
			}
			((NBTTagCompound) nbt).setTag("args", args);
		}else if(!"".equals(this.getText())) {
			nbt= new NBTTagString(this.getText());
		}else {
			nbt = Email.getEmptyTag();
		}
		return nbt;
	}
	
	default void readFromNBT(NBTBase nbt) {
		if(nbt instanceof NBTTagCompound) {
			this.setText(((NBTTagCompound)nbt).getString("key"));
			if(((NBTTagCompound)nbt).hasKey("args")) {
				NBTTagList args = ((NBTTagCompound)nbt).getTagList("args", 8);
				this.setParameters(new Object[args.tagCount()]);
				for(int i = 0; i < this.getParameters().length; i++) {
					NBTBase tag = args.get(i);
					if(tag instanceof NBTTagCompound) {
						this.getParameters()[i] = new InboxText(tag);
					}else {
						this.getParameters()[i] = args.getStringTagAt(i);
					}
				}
			}
		}else if(nbt instanceof NBTTagString) {
			this.setText(((NBTTagString) nbt).getString());
		}
	}
	
	default JsonElement writeToJson() {
		JsonElement e = null;
		
		if(this.getParameters()!=null&&this.getParameters().length>0) {
			e = new JsonObject();
			e.getAsJsonObject().addProperty("key", this.getText());
			JsonArray args = new JsonArray();
			for(int i = 0; i < this.getParameters().length; i++) {
				JsonElement tag;
				if(this.getParameters()[i] instanceof IInboxText) {
					tag = ((IInboxText) this.getParameters()[i]).writeToJson();
				}else {
					tag = new JsonPrimitive(String.valueOf(this.getParameters()[i]));
				}
				args.add(tag);
			}
			e.getAsJsonObject().add("args", args);
		}else if(!"".equals(this.getText())) {
			e = new JsonPrimitive(this.getText());
		}else {
			e = JsonNull.INSTANCE;
		}
		
		return e;
	}
	
	default void readFromJson(JsonElement json) {
		if(json instanceof JsonObject) {
			this.setText(json.getAsJsonObject().get("key").getAsString());
			if(json.getAsJsonObject().has("args")) {
				JsonArray args = json.getAsJsonObject().getAsJsonArray("args");
				Object[] arg = new Object[args.size()];
				for(int i = 0; i < arg.length; i++) {
					JsonElement e = args.get(i);
					if(e.isJsonObject()) {
						arg[i] = new InboxText(e);
					}else {
						arg[i] = e.getAsString();
					}
				}
				this.setParameters(arg);
			}
		}else if(json instanceof JsonPrimitive) {
			this.setText(json.getAsString());
		}
	}
	
	default IInboxText copy() {
		return new InboxText(this.writeToNBT());
	}
}
