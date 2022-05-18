package cxs.idea.plugin;

import com.intellij.openapi.ui.Messages;
import cxs.idea.plugin.file.FileUtil;
import cxs.idea.plugin.rpc.GsonSerializer;
import cxs.idea.plugin.rpc.HttpClientUtil;
import cxs.idea.plugin.rpc.RpcRequest;
import cxs.idea.plugin.rpc.UpLoadClassFileModel;

import java.util.Base64;

/**
 * @author gwk_2
 * @date 2022/5/12 16:06
 */
public class LogicCore {

    public void handle(String ip, String filePath, String fileName, EHot eHotManager) {

        byte[] read = FileUtil.read(filePath);
        if (read == null) {
            Messages.showMessageDialog("热更新读取文件失败", "EHot 提示", Messages.getErrorIcon());
            return;
        }

        RpcRequest<UpLoadClassFileModel> req = new RpcRequest<>();
        req.setCode(1);

        UpLoadClassFileModel upLoadClassFileModel = new UpLoadClassFileModel();
        upLoadClassFileModel.setFileName(fileName);

        String encodeToString = Base64.getEncoder().encodeToString(read);
        upLoadClassFileModel.setClassBase64(encodeToString);

        req.setData(upLoadClassFileModel);

        String pa = String.format("http://%s:4379/upload", ip);

        try {
            HttpClientUtil.postRequest(pa, GsonSerializer.serialize(req));
        } catch (Exception e) {
            Messages.showMessageDialog("热更新失败,msg= " + e.getMessage(), "EHot 提示", Messages.getErrorIcon());
            return;
        }

        Messages.showMessageDialog("热更新成功,文件= " + fileName, "EHot 提示", Messages.getInformationIcon());

        // reset
        eHotManager.lastUpdateFilePath = null;

    }
}
