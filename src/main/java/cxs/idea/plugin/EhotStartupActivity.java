package cxs.idea.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import cxs.idea.plugin.watch.MyListener;
import cxs.idea.plugin.watch.WatcherFactory;
import cxs.idea.plugin.watch.nio.AbstractNIO2Watcher;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class EhotStartupActivity implements StartupActivity {

    private static com.intellij.openapi.diagnostic.Logger LOGGER = com.intellij.openapi.diagnostic.Logger.getInstance(EhotStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOGGER.info("启动 MyStartupActivity");
        final String basePath = project.getBasePath();
        final File file = new File(basePath);
        final URI uri = file.toURI();
        AbstractNIO2Watcher watcher;
        try {
            watcher = (AbstractNIO2Watcher) new WatcherFactory().getWatcher();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        watcher.addEventListener(getClass().getClassLoader(), uri, new MyListener(project));

        if (watcher.getKeys().size() == 0) {
            LOGGER.error("监听的文件 Key 数量为 0");
        } else {
            watcher.run();
            Disposer.register(project, new EhotDispose(watcher));
        }
    }

}
