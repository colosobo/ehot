package cxs.idea.plugin.rpc;

/**
 * @author gwk_2
 * @date 2022/5/12 16:49
 */
public class UpLoadClassFileModel {

    String fileName;

    String classBase64;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getClassBase64() {
        return classBase64;
    }

    public void setClassBase64(String classBase64) {
        this.classBase64 = classBase64;
    }
}
