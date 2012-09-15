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

package org.mobicents.javax.media.mscontrol;

import java.util.HashSet;
import java.util.Set;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.SupportedFeatures;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.join.JoinableStream.StreamType;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.Trigger;

/**
 *
 * @author kulikov
 */
public class MediaConfigImpl implements MediaConfig {

    private SupportedFeaturesImpl features;
    private Parameters params = new ParametersImpl();
    
    public MediaConfigImpl(SupportedFeaturesImpl features, Parameters params) {
        this.features = features;
        this.params.putAll(params);
    }
    
    public boolean hasStream(StreamType streamType) {
        return streamType.equals(StreamType.audio);
    }

    public MediaConfig createCustomizedClone(Parameters params) throws MediaConfigException {
        Parameters cust = new ParametersImpl();
        cust.putAll(this.params);
        
        //create new sets
        Set<Parameter> parameters = new HashSet();
        Set<Action> actions = new HashSet();
        Set<EventType> eventTypes = new HashSet();
        Set<Qualifier> qualifiers = new HashSet();
        Set<Trigger> triggers = new HashSet();
        Set<Value> values = new HashSet();
        
        //copy default values
        parameters.addAll(features.getSupportedParameters());
        actions.addAll(features.getSupportedActions());
        eventTypes.addAll(features.getSupportedEventTypes());
        qualifiers.addAll(features.getSupportedQualifiers());
        triggers.addAll(features.getSupportedTriggers());
        values.addAll(features.getSupportedValues());
        
        //add customized parameters and values
        if (params != null) {
            Set<Parameter> set = params.keySet();
            for (Parameter p : set) {
                parameters.add(p);
            }
        }
        
        SupportedFeaturesImpl f = new SupportedFeaturesImpl(
                parameters, actions, eventTypes, qualifiers, triggers, values);
        
        if (params != null) {
            cust.putAll(params);
        }
        return new MediaConfigImpl(f, cust);
    }

    public SupportedFeatures getSupportedFeatures() {
        return features;
    }

    public Object getValue(Parameter p) {
        return params.get(p);
    }
    
    public Parameters getParameters() {
        return params;
    }
    
    public String marshall() {
        return "";
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
