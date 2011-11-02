/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.javax.media.mscontrol.mixer;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.resource.AllocationEvent;
import javax.media.mscontrol.resource.ResourceContainer;

/**
 *
 * @author kulikov
 */
public class AllocationEventImpl implements AllocationEvent {
    
    private ResourceContainer container;
    private EventType eventType;
    boolean isSuccessful;
    private MediaErr error;
    private String errorText;
            
    public AllocationEventImpl(ResourceContainer container, EventType eventType, 
            boolean isSuccessful,MediaErr error, String errorText ) {
        this.container = container;
        this.eventType = eventType;
        this.isSuccessful = isSuccessful;
        this.error = error;
        this.errorText = errorText;
    }
    
    public ResourceContainer getSource() {
        return container;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public boolean isSuccessful() {
        return this.isSuccessful;
    }

    public MediaErr getError() {
        return this.error;
    }

    public String getErrorText() {
        return this.errorText;
    }
    
}
