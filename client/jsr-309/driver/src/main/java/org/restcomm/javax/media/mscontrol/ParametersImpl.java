package org.restcomm.javax.media.mscontrol;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;

/**
 * 
 * @author amit bhayani
 * 
 */
public class ParametersImpl implements Parameters {

    Map<Parameter, Object> parameters;

    public ParametersImpl() {
        parameters = new HashMap<Parameter, Object>();
    }

    public void clear() {
        parameters.clear();
    }

    public boolean containsKey(Object key) {
        return parameters.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return parameters.containsValue(value);
    }

    public Set<java.util.Map.Entry<Parameter, Object>> entrySet() {
        return parameters.entrySet();
    }

    public Object get(Object key) {
        return parameters.get(key);
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    public Set<Parameter> keySet() {
        return parameters.keySet();
    }

    public Object put(Parameter key, Object value) {
        return parameters.put(key, value);
    }

    public void putAll(Map<? extends Parameter, ? extends Object> t) {
        parameters.putAll(t);
    }

    public Object remove(Object key) {
        return parameters.remove(key);
    }

    public int size() {
        return parameters.size();
    }

    public Collection<Object> values() {
        return parameters.values();
    }
    
    @Override
    public String toString() {
        return this.parameters.toString();
    }
}
