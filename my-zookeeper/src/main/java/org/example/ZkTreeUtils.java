package org.example;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class ZkTreeUtils {

    public static int countDescendants(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
        Stat stat = zk.exists(path, false);
        if (stat == null) return 0;

        List<String> children = zk.getChildren(path, false);
        int count = children.size();

        for (String child : children) {
            count += countDescendants(zk, path + "/" + child);
        }

        return count;
    }

    public static void printTree(ZooKeeper zk, String path, int level) throws KeeperException, InterruptedException {
        if (zk.exists(path, false) == null) return;

        System.out.println("  ".repeat(level) + path);

        List<String> children = zk.getChildren(path, false);
        for (String child : children) {
            printTree(zk, path + "/" + child, level + 1);
        }
    }
}
