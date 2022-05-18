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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * NIO2 watcher implementation.
 * <p/>
 * Java 7 (NIO2) watch a directory (or tree) for changes to files.
 * <p/>
 * By http://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java
 *
 * @author Jiri Bubnik
 * @author alpapad@gmail.com
 */
public class WatcherNIO2 extends AbstractNIO2Watcher {
    private final static WatchEvent.Modifier HIGH;

    static {
        HIGH = getWatchEventModifier("com.sun.nio.file.SensitivityWatchEventModifier", "HIGH");
    }

    public WatcherNIO2() throws IOException {
        super();
    }

    @Override
    protected void registerAll(final Path dir) throws IOException {
        // register directory and sub-directories
        LOGGER.debug("Registering directory  {}", dir);

        List<Path> list = new ArrayList<>();

        Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.endsWith("target")) {
                    list.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        List<String> excludes = new ArrayList<>();
        excludes.add("/target/test-classes");
        excludes.add("/target/maven-status");
        excludes.add("/target/maven-archiver");
        excludes.add("/target/generated-sources");
        excludes.add("/target/generated-test-sources");

        for (Path path : list) {
            Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    for (String exclude : excludes) {
                        if (dir.toFile().getAbsoluteFile().toString().contains(exclude)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        // try to set high sensitivity
        final WatchKey key = HIGH == null ? dir.register(watcher, KINDS) : dir.register(watcher, KINDS, HIGH);
        keys.put(key, dir);
    }
}
