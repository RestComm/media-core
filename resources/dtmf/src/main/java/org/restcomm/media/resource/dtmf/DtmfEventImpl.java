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

package org.restcomm.media.resource.dtmf;

import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfEvent;

/**
 * 
 * @author kulikov
 */
public class DtmfEventImpl implements DtmfEvent {

    private DtmfDetector source;
    private int volume;
    private String tone;
    
    public DtmfEventImpl(DtmfDetector source, String tone, int volume) {
        this.source = source;
        this.volume = volume;
        this.tone = tone;
    }
    
    public int getVolume() {
        return volume;
    }
    

    public DtmfDetector getSource() {
        return source;
    }

    public String getTone() {
        return tone;
    }
}
