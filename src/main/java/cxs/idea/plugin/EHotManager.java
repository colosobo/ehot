package cxs.idea.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

public class EHotManager extends AnAction {


    public final static Map<Project, EHot> CONTAINER = new HashMap<>();

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        Project project = anActionEvent.getProject();
        EHot eHot = CONTAINER.get(project);
        if (eHot == null) {
            eHot = new EHot();
            CONTAINER.put(anActionEvent.getProject(), eHot);
        }
        eHot.handle();
    }
}
