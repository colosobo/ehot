package cxs.idea.plugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import cxs.idea.plugin.EHot;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputDialog extends DialogWrapper {
    public static String pattern = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";

    private JTextField ipInput;
    private String fileName;
    private EHot eHotManager;

    public JTextField getIpInput() {
        return ipInput;
    }

    public InputDialog(String title, String fileName, EHot eHotManager) {
        super(true);
        this.fileName = fileName;
        this.eHotManager = eHotManager;
        init();
        setTitle(title);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel jp = new JPanel();

        jp.setBorder(JBUI.Borders.empty(10));
        jp.setLayout(new GridLayout(3, 2, 10, 10));

        JLabel jl1 = new JLabel("待热部署的 Class 文件:");
        JTextField jl1Text = new JTextField();
        jl1Text.setPreferredSize(new Dimension(400, 28));
        jl1Text.setText(this.fileName);
        jl1Text.setEditable(false);

        JLabel jl2 = new JLabel("热部署的目标 k8s Pod IP:");
        JTextField jl2Text = new JTextField();
        jl2Text.setText(eHotManager.preIP);
        jl2Text.setPreferredSize(new Dimension(400, 28));

        this.ipInput = jl2Text;
        jp.add(jl1);
        jp.add(jl1Text);
        jp.add(jl2);
        jp.add(jl2Text);

        return jp;
    }

    @Nullable
    @Override
    public ValidationInfo doValidate() {
        String text = ipInput.getText();
        if (StringUtils.isBlank(text)) {
            return new ValidationInfo("IP 不能为空");
        }

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);

        if (!m.find()) {
            return new ValidationInfo("请输入合法的 IP 地址.");
        }

        eHotManager.preIP = text;

        return null;
    }
}

