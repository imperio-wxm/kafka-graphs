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
package io.kgraph.library.cf;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the ID of a node in a CF scenario that has an 
 * identifier of type long. 
 *
 * @author dl
 *
 */
public class CfLongId implements CfId<Long>, Comparable<CfId<Long>> {

    private static final Pattern CF_LONG_ID_PATTERN = Pattern.compile("\\((\\d+),\\s*(\\d+)\\)");

    private final byte type;
    private final Long id;

    public CfLongId(byte type, long id) {
        this.type = type;
        this.id = id;
    }

    public CfLongId(String s) {
        Matcher m = CF_LONG_ID_PATTERN.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid string: " + s);
        }
        this.type = Byte.parseByte(m.group(2));
        this.id = Long.parseLong(m.group(1));
    }

    public boolean isItem() {
        return type == 1;
    }

    public boolean isUser() {
        return type == 0;
    }

    public boolean isOutput() {
        return type == -1;
    }

    public byte getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    @Override
    public int compareTo(CfId<Long> that) {
        if (this.type < that.getType()) {
            return -1;
        } else if (this.type > that.getType()) {
            return 1;
        }

        if (this.id.compareTo(that.getId()) < 0) {
            return -1;
        } else if (this.id.compareTo(that.getId()) > 0) {
            return 1;
        }
        return 0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfLongId cfLongId = (CfLongId) o;
        return type == cfLongId.type &&
            Objects.equals(id, cfLongId.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + type;
        return result;
    }

    @Override
    public String toString() {
        return "(" + id + ", " + type + ")";
    }
}
