/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.protocols.mgcp.jain.pkg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Value implements Map<Parameter, Value> {

	Map<Parameter, Value> parameterValueMap = new HashMap<Parameter, Value>();

	public Value() {

	}

	public void clear() {
		parameterValueMap.clear();
	}

	public Set<Entry<Parameter, Value>> entrySet() {
		return parameterValueMap.entrySet();
	}

	public boolean isEmpty() {
		return parameterValueMap.isEmpty();
	}

	public Set<Parameter> keySet() {
		return parameterValueMap.keySet();
	}

	public Value put(Parameter key, Value value) {
		return parameterValueMap.put(key, value);
	}

	public void putAll(Map<? extends Parameter, ? extends Value> t) {
		parameterValueMap.putAll(t);
	}

	public int size() {
		return parameterValueMap.size();
	}

	public Collection<Value> values() {
		return parameterValueMap.values();
	}

	public boolean containsKey(Object key) {
		return parameterValueMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return parameterValueMap.containsKey(value);
	}

	public Value get(Object key) {
		return parameterValueMap.get(key);
	}

	public Value remove(Object key) {
		return parameterValueMap.remove(key);
	}

}
