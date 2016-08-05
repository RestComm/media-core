/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp.util;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Generic parameters map that returns {@link Optional} responses and casting capabilities for ease of use.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @param <K> The key type of the map.
 */
public class Parameters<K> {

    private final Map<K, String> parameters;

    public Parameters() {
        this.parameters = new HashMap<>(10);
    }

    private String get(K key) {
        return this.parameters.get(key);
    }

    private <V> Optional<V> get(K key, Function<String, V> transformer) {
        return Optional.fromNullable(get(key)).transform(transformer);
    }

    public void put(K key, String value) {
        this.parameters.put(key, value);
    }

    public Optional<String> getString(K key) {
        return Optional.fromNullable(get(key));
    }

    public Optional<Integer> getInteger(K key) {
        return get(key, ValueTransformers.STRING_TO_INTEGER);
    }

    public Optional<Integer> getIntegerBase16(K key) {
        return get(key, ValueTransformers.STRING_TO_INTEGER_BASE16);
    }

    public Optional<Long> getLong(K key) {
        return get(key, ValueTransformers.STRING_TO_LONG);
    }

    public Optional<Boolean> getBoolean(K key) {
        return get(key, ValueTransformers.STRING_TO_BOOLEAN);
    }
    
    public void clear() {
        this.parameters.clear();
    }

}
