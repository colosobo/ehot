/*
 * Copyright 2013-2022 the HotswapAgent authors.
 *
 * This file is part of HotswapAgent.
 *
 * HotswapAgent is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 2 of the License, or (at your
 * option) any later version.
 *
 * HotswapAgent is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with HotswapAgent. If not, see http://www.gnu.org/licenses/.
 */
package cxs.idea.plugin.watch.nio;

import cxs.idea.plugin.watch.WatchEventListener;
import cxs.idea.plugin.watch.Watcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * NIO2 watcher implementation for systems which support
 * ExtendedWatchEventModifier.FILE_TREE
 * <p/>
 * Java 7 (NIO2) watch a directory (or tree) for changes to files.
 * <p/>
 * By http://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java
 *
 * @author Jiri Bubnik
 * @author alpapad@gmail.com
 */
public abstract class AbstractNIO2Watcher implements Watcher {
    protected com.intellij.openapi.diagnostic.Logger LOGGER = com.intellij.openapi.diagnostic.Logger.getInstance(this.getClass());

    protected final static WatchEvent.Kind<?>[] KINDS = new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};

    protected WatchService watcher;
    protected final Map<WatchKey, Path> keys;
    private final Map<Path, List<WatchEventListener>> listeners = new ConcurrentHashMap<>();

    // keep track about which classloader requested which event
    protected Map<WatchEventListener, ClassLoader> classLoaderListeners = new ConcurrentHashMap<>();

    private Thread runner;

    private volatile boolean stopped;

    protected final EventDispatcher dispatcher;

    public AbstractNIO2Watcher() throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new ConcurrentHashMap<>();
        dispatcher = new EventDispatcher(listeners);
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public Map<WatchKey, Path> getKeys() {
        return keys;
    }

    @Override
    public synchronized void addEventListener(ClassLoader classLoader, URI pathPrefix, WatchEventListener listener) {
        File path;
        try {
            // check that it is regular file
            // toString() is weird and solves HiarchicalUriException for URI
            // like "file:./src/resources/file.txt".
            path = new File(pathPrefix);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to watch for path {} exception" + pathPrefix, e);
            return;
        }

        try {
            addDirectory(path.toPath());
        } catch (IOException e) {
            LOGGER.error("Unable to watch path with prefix '{}' for changes." + pathPrefix, e);
            return;
        }

        List<WatchEventListener> list = listeners.get(Paths.get(pathPrefix));
        if (list == null) {
            list = new ArrayList<WatchEventListener>();
            listeners.put(Paths.get(pathPrefix), list);
        }
        list.add(listener);

        if (classLoader != null) {
            classLoaderListeners.put(listener, classLoader);
        }
    }

    @Override
    public void addEventListener(ClassLoader classLoader, URL pathPrefix, WatchEventListener listener) {
        if (pathPrefix == null) {
            return;
        }

        try {
            addEventListener(classLoader, pathPrefix.toURI(), listener);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to convert URL to URI " + pathPrefix, e);
        }
    }


    /**
     * Remove all transformers registered with a classloader
     *
     * @param classLoader
     */
    @Override
    public void closeClassLoader(ClassLoader classLoader) {
        for (Iterator<Entry<WatchEventListener, ClassLoader>> entryIterator = classLoaderListeners.entrySet().iterator(); entryIterator.hasNext(); ) {
            Entry<WatchEventListener, ClassLoader> entry = entryIterator.next();
            if (entry.getValue().equals(classLoader)) {
                entryIterator.remove();
                try {
                    for (Iterator<Entry<Path, List<WatchEventListener>>> listenersIterator = listeners.entrySet().iterator(); listenersIterator.hasNext(); ) {
                        Entry<Path, List<WatchEventListener>> pathListenerEntry = listenersIterator.next();
                        List<WatchEventListener> l = pathListenerEntry.getValue();

                        if (l != null) {
                            l.remove(entry.getKey());
                        }

                        if (l == null || l.isEmpty()) {
                            listenersIterator.remove();
                        }

                    }
                } catch (Exception e) {
                    LOGGER.error("Ooops", e);
                }
            }
        }
        // cleanup...
        if (classLoaderListeners.isEmpty()) {
            listeners.clear();
            for (WatchKey wk : keys.keySet()) {
                try {
                    wk.cancel();
                } catch (Exception e) {
                    LOGGER.error("Ooops", e);
                }
            }
            try {
                this.watcher.close();
            } catch (IOException e) {
                LOGGER.error("Ooops", e);
            }
            LOGGER.info("All classloaders closed, released watch service..");
            try {
                // Reset
                this.watcher = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                LOGGER.error("Ooops", e);
            }
        }
        LOGGER.debug("All watch listeners removed for classLoader {}", classLoader);
    }

    /**
     * Registers the given directory
     */
    public void addDirectory(Path path) throws IOException {
        registerAll(path);
    }

    protected abstract void registerAll(final Path dir) throws IOException;

    /**
     * Process all events for keys queued to the watcher
     *
     * @return true if should continue
     * @throws InterruptedException
     */
    private boolean processEvents() throws InterruptedException {

        // wait for key to be signaled
        WatchKey key = watcher.poll(10, TimeUnit.MILLISECONDS);

        if (keys.size() == 0) {
            LOGGER.error("keys.size() 为 0, 需要 compile 后重新打开 project.");
            return false;
        }

        if (key == null) {
            return true;
        }

        Path dir = keys.get(key);

        if (dir == null) {
            LOGGER.info("WatchKey '{}' not recognized " + key);
            return true;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == OVERFLOW) {
                LOGGER.info("WatchKey '{}' overflowed " + key);
                continue;
            }

            // Context for directory entry event is the file name of entry
            WatchEvent<Path> ev = cast(event);
            Path name = ev.context();
            Path child = dir.resolve(name);

            LOGGER.debug("Watch event '{}' on '{}' --> {}", event.kind().name(), child, name);

            dispatcher.add(ev, child);

            // if directory is created, and watching recursively, then
            // register it and its sub-directories
            if (kind == ENTRY_CREATE) {
                try {
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        registerAll(child);
                    }
                } catch (IOException x) {
                    LOGGER.error("Unable to register events for directory {}" + child, x);
                }
            }
        }

        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
            LOGGER.error("Watcher on " + keys.get(key) + " not valid, removing path=" + keys.get(key));
            keys.remove(key); // 可能被 clean 了,
            // all directories are inaccessible
            if (keys.isEmpty()) {
                return false;
            }
            if (classLoaderListeners.isEmpty()) {
                for (WatchKey k : keys.keySet()) {
                    k.cancel();
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {

        runner = new Thread() {
            @Override
            public void run() {
                try {
                    for (; ; ) {
                        if (stopped) {
                            break;
                        }
                        if (!processEvents()) {
                            break;
                        }
                    }
                } catch (InterruptedException x) {

                }
            }
        };
        runner.setDaemon(true);
        runner.setName("HotSwap Watcher");
        runner.start();

        dispatcher.start();
    }

    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * Get a Watch event modifier. These are platform specific and hiden in sun api's
     *
     * @see <a href="https://github.com/HotswapProjects/HotswapAgent/issues/41">
     * Issue#41</a>
     * @see <a href=
     * "http://stackoverflow.com/questions/9588737/is-java-7-watchservice-slow-for-anyone-else">
     * Is Java 7 WatchService Slow for Anyone Else?</a>
     */
    static WatchEvent.Modifier getWatchEventModifier(String claz, String field) {
        try {
            Class<?> c = Class.forName(claz);
            Field f = c.getField(field);
            return (WatchEvent.Modifier) f.get(c);
        } catch (Exception e) {
            return null;
        }
    }
}
