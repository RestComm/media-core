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
package org.restcomm.media.resource.recorder.audio;

import org.restcomm.media.spi.recorder.Recorder;
import org.restcomm.media.spi.recorder.RecorderEvent;

/**
 * Implements events fired by recorder.
 * 
 * @author kulikov
 */
public class RecorderEventImpl implements RecorderEvent {
    
    private int id;
    private Recorder source;
    private int qualifier;
    
    /**
     * Constructs new event.
     * 
     * @param id event type identifier
     * @param source recorder fired this event
     */
    public RecorderEventImpl(int id, Recorder source) {
        this.id = id;
        this.source = source;
    }
    
    /**
     * Gets the event type identifier
     * 
     * @return an integer identifier.
     */
    public int getID() {
        return id;
    }

    /**
     * Gets the recorder fired this event
     * 
     * @return the recorder instance.
     */
    public Recorder getSource() {
        return source;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.recorder.RecorderEvent#getQualifier(). 
     */
    public int getQualifier() {
        return qualifier;
    }
    
    /**
     * Modifies qualifier of this event.
     * 
     * @param qualifier qualifier id.
     */
    public void setQualifier(int qualifier) {
        this.qualifier = qualifier;
    }    
}
