package cxs.idea.plugin.rpc;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

/**
 * @author gwk_2
 * @date 2022/5/12 16:12
 */
public class GsonSerializer {

    static Gson gson = new Gson();

    public static <T> byte[] serialize(T obj){
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T deserialize(byte[] bytes, java.lang.reflect.Type type) {
        return gson.fromJson(new String(bytes), type);
    }
}
