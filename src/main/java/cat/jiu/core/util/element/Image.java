package cat.jiu.core.util.element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.IImage;
import cat.jiu.core.util.timer.Timer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class Image implements IImage {
	private static final Map<Integer, int[]> orderCache = Maps.newHashMap();
	
	protected List<ResourceLocation> imgs;
	/** image display order, like mcmeta {@code frames} */
	protected int[] order;
	/** image delayTick, like mcmeta {@code frametime} */
	protected ITimer delayTick;
	protected long delayMillis;
	protected boolean useMillisToTiming;
	protected boolean isDonePlaying = false;
	protected long currentDelayMillis = 0;
	
	protected ResourceLocation img;
	
	public Image(ResourceLocation img) {
		this(null, null, false, 10, img);
	}
	public static int[] genDefaultOrder(int size) {
		if(!orderCache.containsKey(size) || orderCache.get(size) == null || orderCache.get(size).length != size) {
			int[] order = new int[size];
			for(int i = 0; i < order.length; i++) {
				order[i] = i;
			}
			orderCache.put(size, order);
			return order;
		}
		return orderCache.get(size);
	}
	
	public Image(long delayTick, ResourceLocation... imgs) {
		this(Lists.newArrayList(imgs), genDefaultOrder(imgs.length), false, delayTick, null);
	}
	
	public Image(int[] order, long delayTick, ResourceLocation... imgs) {
		this(Lists.newArrayList(imgs), order, false, delayTick, null);
	}
	
	public Image(List<ResourceLocation> imgs, long delayTick) {
		this(imgs, genDefaultOrder(imgs.size()), false, delayTick, null);
	}
	
	public Image(List<ResourceLocation> imgs, int[] order, long delayTick) {
		this(imgs, order, false, delayTick, null);
	}
	
	// useMsToTiming
	public Image(boolean useMsToTiming, long delay, ResourceLocation... imgs) {
		this(Lists.newArrayList(imgs), genDefaultOrder(imgs.length), useMsToTiming, delay, null);
	}
	
	public Image(int[] order, boolean useMsToTiming, long delay, ResourceLocation... imgs) {
		this(Lists.newArrayList(imgs), order, useMsToTiming, delay, null);
	}
	
	public Image(List<ResourceLocation> imgs, boolean useMsToTiming, long delay) {
		this(imgs, genDefaultOrder(imgs.size()), useMsToTiming, delay, null);
	}
	
	public Image(List<ResourceLocation> imgs, int[] order, boolean useMsToTiming, long delay) {
		this(imgs, order, useMsToTiming, delay, null);
	}

	protected Image(List<ResourceLocation> imgs, int[] order, boolean useMsToTiming, long delay, ResourceLocation img) {
		this.imgs = imgs;
		this.order = order;
		this.img = img;
		this.useMillisToTiming = useMsToTiming;
		if(this.useMillisToTiming) {
			this.delayMillis = delay;
		}else {
			this.delayTick = new Timer(delay);
		}
		if(order!=null) {
			for(int i = 0; i < this.order.length; i++) {
				if(this.order[i] >= imgs.size()) {
					this.order[i] = imgs.size()-1;
				}
			}
		}
	}
	
	public Image(NBTTagCompound nbt) {
		this.read(nbt);
	}
	
	protected boolean isStartTiming = false;
	public void startTiming() {
		if(!isStartTiming && useMillisToTiming) {
			new Thread(()->{
				while(!this.isDonePlaying) {
					try {
						Thread.sleep(this.delayMillis);
						if(!this.isDonePlay()) {
							this.update();
						}
					}catch(InterruptedException e) {}
				}
			}).start();
			this.isStartTiming=true;
		}
	}
	
	public int[] getOrder() {return Arrays.copyOf(order, order.length);}
	public Image setOrder(int[] order) {
		this.order = Arrays.copyOf(order, order.length);
		return this;
	}
	
	public ITimer getDelay() {return delayTick;}
	public long getDelayMillis() {
		return delayMillis;
	}
	public Image setDelay(long delay) {
		if(this.useMillisToTiming) {
			this.delayMillis = delay;
		}else {
			this.delayTick = new Timer(delay);
		}
		return this;
	}
	
	public boolean isUseMillisToTiming() {return this.useMillisToTiming;} 
	public Image setUseMillisToTiming(boolean useMillisToTiming) {
		this.useMillisToTiming = useMillisToTiming;
		return this;
	}
	
	public ResourceLocation getImage() {
		if(this.imgs != null && !this.imgs.isEmpty()) {
			return this.imgs.get(this.order[this.displayIndex]);
		}
		return this.img;
	}
	@Override
	public List<ResourceLocation> getImages() {
		return this.imgs;
	}
	public Image setImg(ResourceLocation img) {
		this.img = img;
		return this;
	}
	public Image setImgs(List<ResourceLocation> imgs) {
		this.imgs = imgs;
		return this;
	}
	public Image setImg(int index, ResourceLocation img) {
		if(this.getImages()!=null) {
			this.getImages().set(index, img);
		}
		return this;
	}
	
	public Image setDonePlay(boolean isDonePlaying) {this.isDonePlaying = isDonePlaying; return this;}
	public boolean hasMoreFrame() {return this.imgs != null && this.imgs.size() > 1;}

	protected int displayIndex = 0;
	public void update() {
		if(this.isUseMillisToTiming()) {
			this.currentDelayMillis++;
		}else {
			this.delayTick.update();
		}
	}
	
	public boolean isDonePlay() {
		if(this.isUseMillisToTiming()) {
			return this.currentDelayMillis >= this.delayMillis;
		}
		return this.delayTick != null ? this.delayTick.isDone() : true;
	}
	
	public Image resetDelay() {
		this.displayIndex++;
		if(this.displayIndex >= this.order.length) {
			this.displayIndex = 0;
		}
		if(this.isUseMillisToTiming()) {
			this.currentDelayMillis = 0;
		}else {
			this.delayTick.reset();
		}
		return this;
	}
	
	@Override
	public Image copy() {
		return new Image(this.writeTo(NBTTagCompound.class));
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Image other = (Image) obj;
		if(img == null) {
			if(other.img != null)
				return false;
		}else if(!img.equals(other.img))
			return false;
		if(imgs == null) {
			if(other.imgs != null)
				return false;
		}else if(!imgs.equals(other.imgs))
			return false;
		if(!Arrays.equals(order, other.order))
			return false;
		return true;
	}
}
