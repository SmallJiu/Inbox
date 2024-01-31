package cat.jiu.email.ui.gui.component;

import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.Collection;

public class Scroll<T extends Collection<?>> {
    public final T collection;
    protected int size = -1;
    protected int[] shows = null;
    protected int[] ids = null;
    protected int page = 0;
    protected int showCount = 5;

    public Scroll(T collection) {
        this.collection = collection;
    }

    public int[] getShows() {
        return shows;
    }
    public int getPage() {
        return page;
    }

    public int getShowCount() {
        return showCount;
    }

    public Scroll<T> setShowCount(int showCount) {
        this.showCount = showCount;
        return this;
    }

    public boolean go(int page) {
        if(this.ids == null) return false;
        if(this.ids.length > this.getShowCount()) {
            this.page += page;

            if(this.page < 0) {
                this.page = 0;
            }
            if(this.page > this.ids.length - this.getShowCount()) {
                this.page = this.ids.length - this.getShowCount();
            }
            int[] shows = Arrays.copyOfRange(this.ids, this.page, this.page + this.getShowCount());
            if (!Arrays.equals(this.shows, shows)) {
                this.shows = shows;
                return true;
            }
            return false;
        }else {
            this.shows = this.ids;
        }
        return false;
    }

    public void init() {
        if(this.size == -1) this.size = collection.size();
        if(this.ids == null && this.size > 0) {
            this.ids = toArray(collection.size());
            this.page = 0;
            this.go(0);
        }
        if(this.size != collection.size()) {
            this.size = collection.size();
            this.ids = toArray(collection.size());
            this.go(0);
        }
    }

    public boolean scroll(int key){
        int page = 0;

        if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            page += 2;
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            page += 1;
        }

        if(key == 120) {
            this.go(-1 - page);
            return true;
        }else if (key == -120){
            this.go(1 + page);
            return true;
        }
        return false;
    }

    static int[] toArray(int size){
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        return array;
    }
}
