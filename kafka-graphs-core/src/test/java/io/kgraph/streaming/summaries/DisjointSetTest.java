/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kgraph.streaming.summaries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;


public class DisjointSetTest {

    private final DisjointSet<Integer> ds = new DisjointSet<>();

    @Before
    public void setup() {
        for (int i = 0; i < 8; i++) {
            ds.union(i, i + 2);
        }
    }

    @Test
    public void testGetMatches() {
        assertEquals(ds.matches().size(), 10);
    }

    @Test
    public void testFind() {
        Integer root1 = ds.find(0);
        Integer root2 = ds.find(1);
        assertNotEquals(root1, root2);

        for (int i = 0; i < 10; i++) {
            assertEquals((i % 2) == 0 ? root1 : root2, ds.find(i));
        }
    }

    @Test
    public void testMerge() {
        DisjointSet<Integer> ds2 = new DisjointSet<>();

        for (int i = 0; i < 8; i++) {
            ds2.union(i, i + 100);
        }

        ds2 = ds2.merge(ds);
        assertEquals(18, ds2.matches().size());

        Set<Integer> treeRoots = new HashSet<>();

        for (int element : ds2.matches().keySet()) {
            treeRoots.add(ds2.find(element));
        }

        assertEquals(2, treeRoots.size());
    }
}