/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol;

import java.util.Set;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.SupportedFeatures;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.Trigger;

/**
 *
 * @author kulikov
 */
public class SupportedFeaturesImpl implements SupportedFeatures {

    private Set<Parameter> parameters;
    private Set<Action> actions;
    private Set<EventType> eventTypes;
    private Set<Qualifier> qualifiers;
    private Set<Trigger> triggers;
    private Set<Value> values;

    public SupportedFeaturesImpl (
            Set<Parameter> parameters,
            Set<Action> actions,
            Set<EventType> eventTypes,
            Set<Qualifier> qualifiers,
            Set<Trigger> triggers,
            Set<Value> values) {
        this.parameters = parameters;
        this.actions = actions;
        this.eventTypes = eventTypes;
        this.qualifiers = qualifiers;
        this.triggers = triggers;
        this.values = values;
    }
    
    public Set<Parameter> getSupportedParameters() {
        return parameters;
    }

    public Set<Action> getSupportedActions() {
        return actions;
    }

    public Set<Trigger> getSupportedTriggers() {
        return triggers;
    }

    public Set<EventType> getSupportedEventTypes() {
        return eventTypes;
    }

    public Set<Qualifier> getSupportedQualifiers() {
        return qualifiers;
    }

    public Set<Value> getSupportedValues() {
        return values;
    }
}
