package cxs.idea.plugin;

import com.intellij.openapi.ui.Messages;
import cxs.idea.plugin.dialog.InputDialog;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author gwk_2
 * @date 2022/5/15 17:49
 */
public class EHot {

    private final LogicCore logicCore = new LogicCore();
    public String lastUpdateFilePath;
    public String preIP;

    public void handle() {

        String filePath = lastUpdateFilePath;
        if (StringUtils.isBlank(filePath)) {
            Messages.showMessageDialog("没找到变更的 Class 文件.", "EHot 提示", Messages.getErrorIcon());
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            Messages.showMessageDialog("文件不存在", "EHot 提示", Messages.getErrorIcon());
            lastUpdateFilePath = null;
            return;
        }

        String fileName = file.getName();

        InputDialog inputDialog = new InputDialog("EHot提示, 请输入 POD IP", fileName,this);

        String ip = null;

        if (inputDialog.showAndGet()) {
            ip = inputDialog.getIpInput().getText();
        }
        if (StringUtils.isBlank(ip)) {
            return;
        }

        logicCore.handle(ip, filePath, fileName, this);
    }
}
