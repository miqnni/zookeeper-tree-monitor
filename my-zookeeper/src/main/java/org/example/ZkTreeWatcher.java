package org.example;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;

public class ZkTreeWatcher implements Watcher {

    private final ZooKeeper zk;
    private final String rootPath;
    private Process externalProcess;

    private static final String EXTERNAL_APP = "gimp";

    public ZkTreeWatcher(ZooKeeper zk, String rootPath) {
        this.zk = zk;
        this.rootPath = rootPath;
    }

    public void startWatching() throws KeeperException, InterruptedException {
        if (zk.exists(rootPath, this) != null) {
            // If the node already exists, treat as if NodeCreated occurred
            System.out.println(rootPath + " already exists at startup.");
            startExternalApp();
            watchRecursively(rootPath);
            printTotalDescendants();
        } else {
            // If the node doesn't exist, set watch to catch its creation
            System.out.println("Waiting for node " + rootPath + " to be created...");
            zk.exists(rootPath, this);
        }
    }

    private void watchRecursively(String path) throws KeeperException, InterruptedException {
        // Set both exists and children watchers
        zk.exists(path, this);
        List<String> children = zk.getChildren(path, this);
        for (String child : children) {
            watchRecursively(path + "/" + child);
        }
    }

    private void printTotalDescendants() throws KeeperException, InterruptedException {
        int total = ZkTreeUtils.countDescendants(zk, rootPath);
        System.out.println("Total descendants of " + rootPath + ": " + total);
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            String path = event.getPath();
            Event.EventType type = event.getType();

            if (path == null) return;

            System.out.println("Event: " + type + " on " + path);

            if (path.equals(rootPath)) {
                if (type == Event.EventType.NodeCreated) {
                    System.out.println(rootPath + " created. Starting recursive watch.");
                    startExternalApp();
                    startWatching();
                } else if (type == Event.EventType.NodeDeleted) {
                    System.out.println(rootPath + " deleted.");
                    stopExternalApp();
                    // Continue watching for re-creation
                    zk.exists(rootPath, this);
                }
            }

            // For other events: re-watch tree if node still exists
            if (zk.exists(rootPath, this) != null) {
                watchRecursively(rootPath);
                printTotalDescendants();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startExternalApp() {
        if (externalProcess == null || !externalProcess.isAlive()) {
            try {
                System.out.println("Starting external application...");
                externalProcess = new ProcessBuilder(EXTERNAL_APP).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopExternalApp() {
        if (externalProcess != null && externalProcess.isAlive()) {
            System.out.println("Stopping external application...");
            externalProcess.destroy();
        }
    }
}
