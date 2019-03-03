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

package io.kgraph.pregel.aggregators;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kgraph.EdgeWithValue;
import io.kgraph.VertexWithValue;
import io.kgraph.pregel.ComputeFunction;

public class VertexCount<K, EV> implements ComputeFunction<K, Long, EV, Long> {
    private static final Logger log = LoggerFactory.getLogger(VertexCount.class);

    private static final String VERTEX_COUNT_AGGREGATOR = "VertexCountAggregator";

    @Override
    public void init(Map<String, ?> configs, InitCallback cb) {
        cb.registerAggregator(VERTEX_COUNT_AGGREGATOR, LongSumAggregator.class);
    }

    @Override
    public void compute(
        int superstep,
        VertexWithValue<K, Long> vertex,
        Iterable<Long> messages,
        Iterable<EdgeWithValue<K, EV>> edges,
        Callback<K, Long, EV, Long> cb
    ) {

        if (superstep == 0) {
            cb.aggregate(VERTEX_COUNT_AGGREGATOR, 1L);
        } else {
            Long count = cb.getAggregatedValue(VERTEX_COUNT_AGGREGATOR);
            cb.setNewVertexValue(count);
            cb.voteToHalt();
        }
    }
}
