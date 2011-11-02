package org.mobicents.javax.media.mscontrol.container;

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
