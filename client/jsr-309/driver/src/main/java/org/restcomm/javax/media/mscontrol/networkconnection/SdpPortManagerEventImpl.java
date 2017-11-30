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
package org.restcomm.javax.media.mscontrol.networkconnection;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.Trigger;

import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;
import org.restcomm.fsm.TransitionHandler;

/**
 * 
 * @author amit.bhayani
 * @author kulikov
 */
public class SdpPortManagerEventImpl implements SdpPortManagerEvent, TransitionHandler, StateEventHandler {

    private SdpPortManagerImpl source = null;
    private EventType eventType = null;
    private boolean isSuccessful = false;

    public SdpPortManagerEventImpl(SdpPortManagerImpl source, EventType eventType) {
        this.source = source;
        this.eventType = eventType;
        this.isSuccessful = true;
    }

    public byte[] getMediaServerSdp() {
        return source.getLocalDescriptor().getBytes();
    }

    public Qualifier getQualifier() {
        // TODO Auto-generated method stub
        return null;
    }

    public Trigger getRTCTrigger() {
        // TODO Auto-generated method stub
        return null;
    }

    public MediaErr getError() {
        return source.connection.error;
    }

    public String getErrorText() {
        return source.connection.errorMsg;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public SdpPortManager getSource() {
        return this.source;
    }

    public boolean isSuccessful() {
        return this.isSuccessful;
    }
    
    @Override
    public String toString() {
        return eventType.toString();
    }

    public void process(State state) {
        source.fireEvent(this);
    }

    public void onEvent(State state) {
        source.fireEvent(this);
    }
}
