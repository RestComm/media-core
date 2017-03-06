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

package org.restcomm.media.spi.recorder;

import org.restcomm.media.spi.listener.Event;

/**
 *
 * @author oifa yulian
 */
public interface RecorderEvent extends Event<Recorder> {
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int FAILED = 3;
    public static final int SPEECH_DETECTED = 4;
    
    public static final int MAX_DURATION_EXCEEDED = 1;
    public static final int NO_SPEECH = 2;
    public static final int SUCCESS = 3;
    
    /**
     * Gets the event type id.
     * 
     * @return event id constant;
     */
    public int getID();
    
    /**
     * Get the event qualifier.
     * 
     * @return the qualifier id.
     */
    public int getQualifier();
}
