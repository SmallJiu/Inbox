package cat.jiu.core.api.element;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.element.Image;


import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import crafttweaker.annotations.ZenRegister;

@ZenRegister
@ZenClass("core.Image")
public interface IImage extends ISerializable {
	public static final IImage DEFAULT_IMAGE = new Image(new ResourceLocation("textures/blocks/bedrock.png")) {
		public Image setUseMillisToTiming(boolean useMillisToTiming) {
			return this;
		}
	};
	
	@ZenMethod
	void startTiming();
	
	@ZenGetter("order")
	int[] getOrder();
	@ZenMethod("order")
	IImage setOrder(int[] order);

	@ZenGetter("delay")
	ITimer getDelay();
	@ZenGetter("delay_ms")
	long getDelayMillis();
	@ZenMethod("delay")
	IImage setDelay(long delay);

	@ZenGetter("timingForMS")
	boolean isUseMillisToTiming();
	@ZenMethod("timingForMS")
	IImage setUseMillisToTiming(boolean useMillisToTiming);

	ResourceLocation getImage();
	@ZenGetter("image")
	default String getImageAsString() {
		return String.valueOf(this.getImage()).toLowerCase();
	}
	IImage setImg(ResourceLocation img);
	@ZenMethod("image")
	default IImage setImg(String img) {
		return this.setImg(new ResourceLocation(img));
	}

	@ZenGetter("images")
	List<ResourceLocation> getImages();
	IImage setImgs(List<ResourceLocation> imgs);
	IImage setImg(int index, ResourceLocation img);
	
	@ZenMethod("images")
	default IImage setImgsAsString(List<String> imgs) {
		return this.setImgs(imgs.stream().map(ResourceLocation::new).collect(Collectors.toList()));
	}
	@ZenMethod("images")
	default IImage setImg(int index, String img) {
		return this.setImg(index, new ResourceLocation(img));
	}

	@ZenGetter("done")
	boolean isDonePlay();
	@ZenMethod("done")
	IImage setDonePlay(boolean isDonePlaying);

	@ZenGetter("hasNext")
	boolean hasMoreFrame();
	
	@ZenMethod
	void update();
	@ZenMethod
	IImage resetDelay();
	
	@ZenMethod
	IImage copy();

	@ZenMethod
	default NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		
		if(this.hasMoreFrame()) {
			NBTTagList imgsTag = new NBTTagList();
			
			for(int i = 0; i < this.getImages().size(); i++) {
				imgsTag.appendTag(new NBTTagString(this.getImages().get(i).toString()));
			}
			nbt.setTag("imgs", imgsTag);
			nbt.setTag("order", new NBTTagIntArray(this.getOrder()));
			nbt.setBoolean("useMillis", this.isUseMillisToTiming());
			nbt.setLong("delay", this.isUseMillisToTiming() ? this.getDelayMillis() : this.getDelay().getAllTicks());
		}else {
			nbt.setString("img", this.getImage().toString());
		}
		return nbt;
	}
	@ZenMethod
	default void read(NBTTagCompound nbt) {
		if(nbt.hasKey("imgs")) {
			List<ResourceLocation> imgs = Lists.newArrayList();
			NBTTagList imgsTag = nbt.getTagList("imgs", 8);
			for(int i = 0; i < imgsTag.tagCount(); i++) {
				imgs.add(new ResourceLocation(imgsTag.getStringTagAt(i)));
			}
			this.setImgs(imgs);
			this.setOrder(nbt.getIntArray("order"));
			
			this.setUseMillisToTiming(nbt.getBoolean("useMillis"));
			this.setDelay(nbt.getLong("delay"));
		}else {
			if(nbt.hasKey("img") && !nbt.getString("img").equalsIgnoreCase(DEFAULT_IMAGE.getImage().toString())) {
				this.setImg(new ResourceLocation(nbt.getString("img")));
			}
		}
	}

	@ZenMethod
	@Override
	default JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		
		if(this.hasMoreFrame()) {
			JsonArray imgsArray = new JsonArray();
			
			for(int i = 0; i < this.getImages().size(); i++) {
				imgsArray.add(this.getImages().get(i).toString());
			}
			json.add("imgs", imgsArray);
			
			JsonArray orderArray = new JsonArray();
			for(int i = 0; i < this.getOrder().length; i++) {
				orderArray.add(this.getOrder()[i]);
			}
			json.add("order", orderArray);
			
			json.addProperty("useMillis", this.isUseMillisToTiming());
			json.addProperty("delay", this.isUseMillisToTiming() ? this.getDelayMillis() : this.getDelay().getAllTicks());
		}else {
			json.addProperty("img", this.getImage().toString());
		}
		
		return json;
	}
	@ZenMethod
	@Override
	default void read(JsonObject json) {
		if(json.has("imgs")) {
			List<ResourceLocation> imgs = Lists.newArrayList();
			
			JsonArray imgsArray = json.getAsJsonArray("imgs");
			for(int i = 0; i < imgsArray.size(); i++) {
				imgs.add(new ResourceLocation(imgsArray.get(i).getAsString()));
			}
			this.setImgs(imgs);

			JsonArray orderArray = json.getAsJsonArray("order");
			int[] order = new int[orderArray.size()];
			for(int i = 0; i < order.length; i++) {
				order[i] = orderArray.get(i).getAsInt();
			}
			this.setOrder(order);
			
			this.setUseMillisToTiming(json.get("useMillis").getAsBoolean());
			this.setDelay(json.get("delay").getAsLong());
		}else {
			if(json.has("img") && !json.get("img").getAsString().equalsIgnoreCase(DEFAULT_IMAGE.getImage().toString())) {
				this.setImg(new ResourceLocation(json.get("img").getAsString()));
				this.setUseMillisToTiming(true);
				this.setDelay(0);
			}
		}
	}
	
	@ZenRegister
	@ZenClass("core.image.Builder")
	public static class Builder {
		protected List<ResourceLocation> images;
		protected int[] order;
		protected long delayMillis = 20;
		protected boolean useMillisToUpdata = true;
		
		public Builder addImage(ResourceLocation image) {
			if(this.images==null) this.images = Lists.newArrayList();
			this.images.add(image);
			return this;
		}
		@ZenMethod("image")
		public Builder addImage(String image) {
			return this.addImage(new ResourceLocation(image));
		}
		@ZenMethod("order")
		public Builder setOrder(int... order) {
			this.order = order;
			return this;
		}
		@ZenMethod("orderIndex")
		public Builder setOrderIndex(int index, int image) {
			if(this.order!=null) {
				this.order[index] = image;
			}
			return this;
		}
		@ZenMethod("delay")
		public Builder setDelayMillis(long delayMillis) {
			this.delayMillis = delayMillis;
			return this;
		}
		@ZenMethod("msUpdata")
		public Builder setUseMillisToUpdata(boolean useMillisToUpdata) {
			this.useMillisToUpdata = useMillisToUpdata;
			return this;
		}
		
		@ZenMethod
		public IImage build() {
			if(this.images!=null && this.images.size()>0) {
				if(this.images.size() > 1) {
					int[] order = this.order != null ? this.order : Image.genDefaultOrder(this.images.size());
					return new Image(this.images, order, this.useMillisToUpdata, this.delayMillis);
				}else {
					return new Image(this.images.get(0));
				}
			}
			return DEFAULT_IMAGE;
		}
	}
}
