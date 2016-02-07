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

package org.mobicents.media.server.spi.dtmf;

import org.mobicents.media.MediaSink;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author kulikov
 */
public interface DtmfDetector extends MediaSink {
    /**
     * Default level of the DTMF tone in decibells.
     */
    public final static int DEFAULT_SIGNAL_LEVEL = -30;
    /**
     * Default interdigit time interval in millisconds.
     */
    public final static int DEFAULT_INTERDIGIT_INTERVAL = 500;

    /**
     * The time the system will wait between DTMF digits. 
     * If this value is reached, the system fires dtmf event.
     * 
     * @param interval the time interval in millisconds.
     */
    public void setInterdigitInterval(int interval);

    /**
     * The time the system will wait between DTMF digits. 
     * If this value is reached, the system fires dtmf event.
     * 
     * @return  the time interval in millisconds.
     */
    public int getInterdigitInterval();

    /**
     * Describes the power level of the tone, expressed in dBm0
     * 
     * @param level the value in dBm0
     */
    public void setVolume(int level);

    /**
     * Describes the power level of the tone, expressed in dBm0
     * 
     * @return the value in dBm0
     */
    public int getVolume();
    
    /**
     * Starts media processing.
     */
    public void activate();
    
    /**
     * Terminates media processing.
     */
    public void deactivate();
    
    /**
     * Flushes buffer.
     */
    public void flushBuffer();
    
    /**
     * Clears buffer content.
     */
    public void clearDigits();
    
    public void addListener(DtmfDetectorListener listener) throws TooManyListenersException;
    public void removeListener(DtmfDetectorListener listener);
    public void clearAllListeners();
}
