package cat.jiu.email.element;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.configs.EmailConfigServer;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.IdF;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BlackAndWhiteList {
    public static final String blackKey = "BlackList", whiteKey = "WhiteList";

    public final Map<String, String> black = new HashMap<>(), white = new HashMap<>();

    public BlackAndWhiteList(IData.IMapData<?> data) {
        this.read(data);
    }
    public BlackAndWhiteList() throws Exception {
        this.read(JsonData.readMap(EmailAPI.globalEmailListPath, EmailConfigServer.File_Charset.get()));
    }

    public String remove(String name, boolean black) {
        Map<String, String> map = (black ? this.black : this.white);
        if (map.containsKey(name)) {
            return map.remove(name);
        }else {
            for (String s : new HashSet<>(map.keySet())) {
                if (map.get(s).equals(name)) {
                    return map.remove(s);
                }
            }
        }
        return null;
    }
    public String add(String name, String uid, boolean black) {
        return (black ? this.black : this.white).put(name, uid);
    }
    public boolean contains(String name, boolean uid, boolean black) {
        Map<String, String> map = (black ? this.black : this.white);
        return uid ? map.containsValue(name) : map.containsKey(name);
    }

    public IData.IMapData<?> save() throws Exception {
        return this.save(JsonData.readMap(EmailAPI.globalEmailListPath, EmailConfigServer.File_Charset.get()));
    }
    public IData.IMapData<?> save(IData.IMapData<?> data) {
        if (!this.black.isEmpty()) {
            IData.IMapData<?> map = data.newMap();
            this.black.forEach(map::putData);
            data.putData(blackKey, map);
        }
        if (!this.white.isEmpty()) {
            IData.IMapData<?> map = data.newMap();
            this.white.forEach(map::putData);
            data.putData(whiteKey, map);
        }
        return data;
    }

    public void read(IData.IMapData<?> data) {
        data.getMap(blackKey).foreach((k,v)->this.black.put(k, v.getAsPrimitive().getAsString()));
        data.getMap(whiteKey).foreach((k,v)->this.white.put(k, v.getAsPrimitive().getAsString()));
    }
}
