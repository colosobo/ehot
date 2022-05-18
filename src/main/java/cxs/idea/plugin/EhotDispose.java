package cxs.idea.plugin;

import com.intellij.openapi.Disposable;
import cxs.idea.plugin.watch.nio.AbstractNIO2Watcher;

/**
 * @author gwk_2
 * @date 2022/5/15 16:31
 */
public class EhotDispose implements Disposable {


    AbstractNIO2Watcher watcher;
    public EhotDispose(AbstractNIO2Watcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void dispose() {
        watcher.stop();

    }
}
