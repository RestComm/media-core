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
package org.restcomm.javax.media.mscontrol.container;

import java.io.Serializable;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.JoinEvent;
import javax.media.mscontrol.join.Joinable;

/**
 * 
 * @author amit bhayani
 * 
 */
public class JoinEventImpl implements JoinEvent {

    private Serializable context = null;
    
    private Joinable source = null;
    private Joinable other = null;
    
    private EventType eventType = null;
    
    private MediaErr error = MediaErr.NO_ERROR;
    private String errorText = null;
    
    private boolean isSuccessful = false;

    public JoinEventImpl(MediaObject source, Serializable context, Joinable other, EventType eventId,
            boolean isSuccessful) {
        this.source = (Joinable) source;
        this.context = context;
        this.other = other;
        this.eventType = eventId;
        this.isSuccessful = isSuccessful;
    }

    public JoinEventImpl(MediaObject source, Serializable context, Joinable other, EventType eventID,
            boolean isSuccessful, MediaErr error, String errorText) {
        this(source, context, other,eventID, isSuccessful);
        this.error = error;
        this.errorText = errorText;
    }

    public Serializable getContext() {
        return this.context;
    }

    public MediaErr getError() {
        return this.error;
    }

    public String getErrorText() {
        return this.errorText;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public Joinable getOtherJoinable() {
        return this.other;
    }

    public Joinable getSource() {
        return source;
    }

    public Joinable getThisJoinable() {
        return source;
    }

    @Override
    public String toString() {
        return "Source = " + this.source + " Other = " + this.other + " EventId = " + this.eventType + " Error = " + this.error + " ErrorText = " + this.errorText;
    }

    public boolean isSuccessful() {
        return this.isSuccessful;
    }

}
