/*
 * Copyright 2012-2016 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.vm.byteman.agent.internal.typeadapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;

public class BytemanMetricTypeAdapter extends TypeAdapter<BytemanMetric> {

    @Override
    public BytemanMetric read(JsonReader reader) throws IOException {
        // handle null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        
        reader.beginObject();
        BytemanMetric metric = parseMetric(reader);
        reader.endObject();
        return metric;
    }

    @Override
    public void write(JsonWriter writer, BytemanMetric metric) throws IOException {
        throw new RuntimeException("Not implemented");
    }
    
    @SuppressWarnings("unchecked")
    private BytemanMetric parseMetric(JsonReader reader) throws IOException {
        Map<String, Object> values = new HashMap<>();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch(name) {
            case BytemanMetric.MARKER_NAME:
                values.put(BytemanMetric.MARKER_NAME, reader.nextString());
                break;
            case BytemanMetric.DATA_NAME:
                reader.setLenient(true);
                Map<String, String> data = getDataValues(reader);
                values.put(BytemanMetric.DATA_NAME, data);
                reader.setLenient(false);
                break;
            default:
                throw new IllegalStateException("Unknown name: '" + name + "'");
            }
        }
        BytemanMetric metric = new BytemanMetric();
        metric.setMarker((String)values.get(BytemanMetric.MARKER_NAME));
        metric.setData(mapToJson((Map<String, String>)values.get(BytemanMetric.DATA_NAME)));
        return metric;
    }

    private String mapToJson(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Entry<String, String> entry: map.entrySet()) {
            builder.append("\"").append(entry.getKey()).append("\"");
            builder.append(":");
            builder.append("\"").append(entry.getValue()).append("\"");
            builder.append(",");
        }
        if (map.size() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("}");
        return builder.toString();
    }

    private Map<String, String> getDataValues(JsonReader reader) throws IOException {
        reader.beginObject();
        Map<String, String> data = new HashMap<>();
        while (reader.hasNext()) {
            String name = reader.nextName();
            JsonToken token = reader.peek();
            String value;
            switch (token) {
            case BOOLEAN:
                value = Boolean.toString(reader.nextBoolean());
                break;
            case NULL:
                reader.nextNull();
                value = null;
                break;
            case NUMBER: // fall-through
            case STRING:
                value = reader.nextString();
                break;
            default:
                throw new IllegalStateException("Cannot handle token " + token);
            }
            data.put(name, value);
        }
        reader.endObject();
        return data;
    }

}
