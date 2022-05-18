package cxs.idea.plugin.file;

import java.io.FileInputStream;

/**
 * @author gwk_2
 * @date 2022/5/12 16:46
 */
public class FileUtil {

    public static byte[] read(String path) {
        try {
            FileInputStream in =new FileInputStream(path);
            //当文件没有结束时，每次读取一个字节显示
            byte[] data=new byte[in.available()];
            in.read(data);
            in.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
