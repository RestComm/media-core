/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
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
