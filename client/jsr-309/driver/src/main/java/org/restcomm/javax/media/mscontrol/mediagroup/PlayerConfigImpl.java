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

package org.restcomm.javax.media.mscontrol.mediagroup;

import java.util.HashSet;
import java.util.Set;
import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.Trigger;

import org.restcomm.javax.media.mscontrol.MediaConfigImpl;
import org.restcomm.javax.media.mscontrol.MediaObjectImpl;
import org.restcomm.javax.media.mscontrol.ParametersImpl;
import org.restcomm.javax.media.mscontrol.SupportedFeaturesImpl;

/**
 *
 * @author kulikov
 */
public class PlayerConfigImpl implements Configuration {
    
    private MediaConfigImpl config;
    private Parameters params = new ParametersImpl();
    
    protected PlayerConfigImpl() {
        
        //specify parameters
        Set<Parameter> parameters = new HashSet();
        parameters.add(MediaObject.MEDIAOBJECT_ID);
        parameters.add(Player.BEHAVIOUR_IF_BUSY);
        parameters.add(Player.MAX_DURATION);
        parameters.add(Player.INTERVAL);
        parameters.add(Player.REPEAT_COUNT);
        parameters.add(Player.START_OFFSET);
        
        //specify event types
        Set<EventType> eventTypes = new HashSet();
        eventTypes.add(PlayerEvent.PAUSED);
        eventTypes.add(PlayerEvent.PLAY_COMPLETED);
        eventTypes.add(PlayerEvent.RESUMED);
        eventTypes.add(PlayerEvent.SPEED_CHANGED);
        eventTypes.add(PlayerEvent.VOLUME_CHANGED);
        
        //Define actions
        Set<Action> actions = new HashSet();
        
        //Define qualifiers
        Set<Qualifier> qualifiers = new HashSet();
        qualifiers.add(PlayerEvent.DURATION_EXCEEDED);
        qualifiers.add(PlayerEvent.END_OF_PLAY_LIST);
        
        //Define triggers
        Set<Trigger> triggers = new HashSet();
        
        //Define values
        Set<Value> values = new HashSet();
        values.add(Player.FAIL_IF_BUSY);
        values.add(Player.QUEUE_IF_BUSY);
        values.add(Player.STOP_IF_BUSY);
        
        params.put(MediaObjectImpl.ENDPOINT_NAME, "mobicents/ivr/$");
        
        SupportedFeaturesImpl features = new SupportedFeaturesImpl(parameters, actions, eventTypes, qualifiers, triggers, values);
        config = new MediaConfigImpl(features, params);
    }
    
    public MediaConfig getConfig() {
        return config;
    }
}
