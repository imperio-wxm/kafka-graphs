/*
 * Copyright 2014 Grafos.ml
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kgraph.library.maxbmatching;

import java.util.Objects;

import io.kgraph.library.maxbmatching.MBMEdgeValue.State;

public class MBMMessage {
    private final Long vertexID;
    private final State state;

    public MBMMessage() {
        this.vertexID = 0L;
        this.state = State.DEFAULT;
    }

    public MBMMessage(Long id, State proposed) {
        this.vertexID = id;
        this.state = proposed;
    }

    public Long getId() {
        return vertexID;
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "(" + vertexID + ", " + state + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MBMMessage that = (MBMMessage) o;
        return Objects.equals(vertexID, that.vertexID) &&
            state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexID, state);
    }
}
