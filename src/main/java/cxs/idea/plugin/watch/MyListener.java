package cxs.idea.plugin.watch;

import com.intellij.openapi.project.Project;
import cxs.idea.plugin.EHot;
import cxs.idea.plugin.EHotManager;

import java.io.File;
import java.net.URI;

public class MyListener implements WatchEventListener {

    Project project;

    public MyListener(Project project) {
        this.project = project;
    }

    @Override
    public void onEvent(WatchFileEvent event) {
        final URI uri = event.getURI();
        final File file = new File(uri);
        final String path = file.getPath();
        if (!path.endsWith(".class")) {
            // 只处理 class 文件.
            return;
        }
        EHot eHot = EHotManager.CONTAINER.get(project);
        if (eHot == null) {
            eHot = new EHot();
            EHotManager.CONTAINER.put(project, eHot);
        }
        eHot.lastUpdateFilePath = path;

    }
}
