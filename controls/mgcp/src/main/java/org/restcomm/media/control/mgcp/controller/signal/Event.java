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
package org.restcomm.media.control.mgcp.controller.signal;

import java.util.ArrayList;

import org.restcomm.media.spi.utils.Text;
/**
 * Event thrown by endpoint or connection.
 * 
 * Event has one more associated handlers or action. When <code>match(Text)</code> 
 * is called the requested action is selected for execution when this event will 
 * be detected.
 * 
 * @author kulikov
 */
public class Event {

    //the name of this event
    private Text name;
    
    //descriptor of requested action for this event.
    private Text evtName = new Text();    
    private Text actionName = new Text();    
    private Text[] descriptor = new Text[] {evtName, actionName};
    
    //indicates that this event was requested by controller
    private boolean isActive;

    //supported actions
    private ArrayList<EventAction> actions = new ArrayList<EventAction>();
    
    //selected action
    private EventAction requestedAction;
    
    /**
     * Constructs new event name.
     * 
     * @param eventName 
     */
    public Event(Text eventName) {
        this.name = eventName;
    }
    
    /**
     * Gets the name of this event.
     * 
     * @return name of this event.
     */
    public Text getName() {
        return name;        
    }
    
    /**
     * Adds handlers for this event.
     * 
     * @param action the event handler.
     */
    public void add(EventAction action) {
        this.actions.add(action);
    }
    
    /**
     * Checks that this event objects matches to the specified event descriptor.
     * 
     * @param event the event descriptor which includes the event name and 
     * requested action.
     * 
     * @return true if event name specified in descriptor matches to this 
     * name of this event object.
     */
    public boolean matches(Text eventDescriptor) {
        //clean current requested action
        this.requestedAction = null;
        
        //parse event descriptor
        eventDescriptor.divide(new char[]{'(', ')'}, descriptor);
        
        evtName.trim();
        actionName.trim();
        
        //check name first
        this.isActive = evtName.equals(this.name);
        if (!this.isActive) {
            return false;
        }
        
        //select action
        for (EventAction a : actions) {
            if (a.getName().equals(actionName)) {
                this.requestedAction = a;
                break;
            }
        }
        
        this.isActive = this.requestedAction != null;
        //this event matches to the specified descriptor if 
        //name match and requested action selected
        return this.isActive;
    }
    
    /**
     * Is this event requested or not.
     * 
     * @return true if this event requested for detection or false otherwise.
     */
    public boolean isActive() {
        return this.isActive;
    }
    
    /**
     * Executes action associated with this event
     */
    public void fire(Signal s, Text options) {
    	if (this.isActive) {    		
            this.requestedAction.perform(s, this, options);
        }    	
    }
    
    public void reset() {
        this.isActive = false;
        this.requestedAction = null;
    }
}
