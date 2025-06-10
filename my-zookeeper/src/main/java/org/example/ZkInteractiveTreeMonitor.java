package org.example;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ZkInteractiveTreeMonitor {

    private static final String ZNODE_A = "/a";
    private ZooKeeper zk;
    private ZkTreeWatcher treeWatcher;

    public static void main(String[] args) throws Exception {
        ZkInteractiveTreeMonitor app = new ZkInteractiveTreeMonitor();
        app.connect("localhost:2181,localhost:2182,localhost:2183");

        app.treeWatcher = new ZkTreeWatcher(app.zk, ZNODE_A);
        app.treeWatcher.startWatching();

        // Start CLI thread
        Thread cliThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                switch (input) {
                    case "tree":
                        try {
                            System.out.println("Tree under /a:");
                            ZkTreeUtils.printTree(app.zk, ZNODE_A, 0);
                        } catch (Exception e) {
                            System.err.println("Error printing tree: " + e.getMessage());
                        }
                        break;
                    case "descendants":
                        try {
                            int total = ZkTreeUtils.countDescendants(app.zk, ZNODE_A);
                            System.out.println("Total descendants of /a: " + total);
                        } catch (Exception e) {
                            System.err.println("Error counting descendants: " + e.getMessage());
                        }
                        break;
                    case "exit":
                        System.out.println("Shutting down...");
                        System.exit(0);
                    default:
                        System.out.println("Available commands: tree, descendants, exit");
                }
            }
        });
        cliThread.setDaemon(true); // Allows clean shutdown if main exits
        cliThread.start();

        // Keep the main thread alive to handle ZooKeeper events
        Thread.sleep(Long.MAX_VALUE);
    }

    public void connect(String hostPort) throws IOException, InterruptedException {
        CountDownLatch connectedSignal = new CountDownLatch(1);
        zk = new ZooKeeper(hostPort, 3000, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();
        System.out.println("Connected to ZooKeeper.");
    }
}
