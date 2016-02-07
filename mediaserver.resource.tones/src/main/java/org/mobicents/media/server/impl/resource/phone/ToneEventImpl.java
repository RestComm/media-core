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

package org.mobicents.media.server.impl.resource.phone;

import org.mobicents.media.server.spi.tone.ToneDetector;
import org.mobicents.media.server.spi.tone.ToneEvent;

/**
 * 
 * @author Oifa Yulian
 */
public class ToneEventImpl implements ToneEvent {

    private ToneDetector source;
    private int frequency;
    
    public ToneEventImpl(ToneDetector source,int frequency) {
        this.source = source;
        this.frequency=frequency;
    }
    
    public ToneDetector getSource() {
        return source;
    }    
    
    public int getFrequency()
    {
    	return this.frequency;
    }
}
