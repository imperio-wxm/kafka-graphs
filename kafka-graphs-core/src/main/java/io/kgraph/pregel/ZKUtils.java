/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kgraph.pregel;

import java.util.Map;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kgraph.GraphAlgorithmState;

public class ZKUtils {
    private static final Logger log = LoggerFactory.getLogger(ZKUtils.class);

    public static final String GRAPHS_PATH = "/kafka-graphs";
    public static final String PREGEL_PATH = "/kafka-graphs/pregel-";

    public static final String AGGREGATES = "aggregates";
    public static final String BARRIERS = "barriers";
    public static final String GROUP = "group";
    public static final String LEADER = "leader";
    public static final String READY = "ready";
    public static final String SUPERSTEP = "superstep";

    public static CuratorFramework createCurator(String zookeeperConnect) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework curator = CuratorFrameworkFactory.newClient(zookeeperConnect, retryPolicy);
        curator.start();
        return curator;
    }

    public static PregelState maybeCreateReadyToSendNode(
        CuratorFramework curator, String id, PregelState pregelState, TreeCache treeCache, int groupSize) throws Exception {
        if (pregelState.superstep() < 0) {
            return pregelState.next();
        }
        String barrierPath = barrierPath(id, pregelState);
        Map<String, ChildData> children = treeCache.getCurrentChildren(barrierPath);
        if (children == null) {
            return pregelState;
        }
        int childrenSize = children.size();
        if (children.containsKey(READY)) childrenSize--;
        if (childrenSize == groupSize) {
            String nextBarrierPath = barrierPath(id, pregelState.next());
            Map<String, ChildData> nextChildren = treeCache.getCurrentChildren(nextBarrierPath);
            if (nextChildren == null) {
                return pregelState.state(GraphAlgorithmState.State.COMPLETED);
            }
            int nextChildrenSize = nextChildren.size();
            if (nextChildren.containsKey(READY)) nextChildrenSize--;
            if (nextChildrenSize == 0) {
                return pregelState.state(GraphAlgorithmState.State.COMPLETED);
            } else {
                // only advance superstep if there is more work to do
                addReady(curator, id, pregelState);
                return pregelState.next();
            }
        }
        return pregelState;
    }

    public static PregelState maybeCreateReadyToReceiveNode(
        CuratorFramework curator, String id, PregelState pregelState, TreeCache treeCache) throws Exception {
        if (pregelState.superstep() < 0) {
            return pregelState.next();
        }
        String barrierPath = barrierPath(id, pregelState);
        Map<String, ChildData> children = treeCache.getCurrentChildren(barrierPath);
        if (children == null) {
            return pregelState;
        }
        int childrenSize = children.size();
        if (children.containsKey(READY)) childrenSize--;
        if (childrenSize == 0) {
            addReady(curator, id, pregelState);
            return pregelState.next();
        }
        return pregelState;
    }

    private static void addReady(CuratorFramework curator, String id, PregelState pregelState) throws Exception {
        String nextBarrierPath = barrierPath(id, pregelState.next());
        String path = ZKPaths.makePath(nextBarrierPath, READY);
        try {
            log.debug("adding ready {}", path);
            curator.create().creatingParentContainersIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            // ignore
        }
    }

    public static boolean isReady(CuratorFramework curator, String id, PregelState pregelState) throws Exception {
        String barrierPath = barrierPath(id, pregelState);
        String path = ZKPaths.makePath(barrierPath, READY);
        boolean exists = curator.checkExists().forPath(path) != null;
        return exists;
    }

    public static boolean hasChild(CuratorFramework curator, String id, PregelState pregelState,
                                   String child) throws Exception {
        String barrierPath = barrierPath(id, pregelState);
        return hasChild(curator, barrierPath, child);
    }

    public static boolean hasChild(CuratorFramework curator, String rootPath,
                                   String child) throws Exception {
        String path = ZKPaths.makePath(rootPath, child);
        boolean exists = curator.checkExists().forPath(path) != null;
        return exists;
    }

    public static void addChild(CuratorFramework curator, String id, PregelState pregelState,
                                String child) throws Exception {
        addChild(curator, id, pregelState, child, CreateMode.PERSISTENT);
    }

    public static void addChild(CuratorFramework curator, String id, PregelState pregelState,
                                String child, CreateMode createMode) throws Exception {
        addChild(curator, barrierPath(id, pregelState), child, createMode);
    }

    public static void addChild(CuratorFramework curator, String rootPath,
                                String child, CreateMode createMode) throws Exception {
        addChild(curator, rootPath, child, createMode, new byte[0]);
    }

    public static void addChild(CuratorFramework curator, String rootPath,
                                String child, CreateMode createMode, byte[] data) throws Exception {
        String path = ZKPaths.makePath(rootPath, child);
        try {
            log.debug("adding child {}", path);
            curator.create().creatingParentContainersIfNeeded().withMode(createMode).forPath(path, data);
        } catch (KeeperException.NodeExistsException e) {
            if (data.length != 0) {
                log.warn("could not add child {}", path);
            }
        }
    }

    public static byte[] getChildData(CuratorFramework curator, String rootPath, String child)
        throws Exception {
        return curator.getData().forPath(ZKPaths.makePath(rootPath, child));
    }

    public static void updateChild(CuratorFramework curator, String rootPath,
                                   String child, byte[] data) throws Exception {
        String path = ZKPaths.makePath(rootPath, child);
        log.debug("adding child {}", path);
        curator.setData().forPath(path, data);
    }

    public static void removeChild(CuratorFramework curator, String id, PregelState pregelState,
                                   String child) throws Exception {
        removeChild(curator, barrierPath(id, pregelState), child);
    }

    public static void removeChild(CuratorFramework curator, String rootPath,
                                   String child) throws Exception {
        String path = ZKPaths.makePath(rootPath, child);
        try {
            log.debug("removing child {}", path);
            curator.delete().guaranteed().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            // ignore
        }
    }

    public static void removeRoot(CuratorFramework curator, String id) throws Exception {
        String path = PREGEL_PATH + id;
        try {
            log.debug("removing root {}", path);
            curator.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            // ignore
        }
    }

    protected static String aggregatePath(String id, int superstep) {
        return ZKPaths.makePath(PREGEL_PATH + id, AGGREGATES, String.valueOf(superstep));
    }

    protected static String barrierPath(String id, PregelState pregelState) {
        return ZKPaths.makePath(
            PREGEL_PATH + id, BARRIERS,
            (pregelState.stage() == PregelState.Stage.RECEIVE ? "rcv-" : "snd-") + pregelState.superstep());
    }
}
