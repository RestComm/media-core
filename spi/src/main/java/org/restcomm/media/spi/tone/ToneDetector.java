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

package org.restcomm.media.spi.tone;

import org.restcomm.media.MediaSink;
import org.restcomm.media.spi.listener.TooManyListenersException;

/**
 *
 * @author Oifa Yulian
 */
public interface ToneDetector extends MediaSink {
    /**
     * Set frequencies of tone that detector should search for
     * If tone detected the system fires tone event.
     * 
     */
    public void setFrequency(int[] f);
    
    /**
     * Gets the frequencies     
     * 
     * @return  the list of frequencies
     */
    public int[] getFrequency();
    
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
    
    public void addListener(ToneDetectorListener listener) throws TooManyListenersException;
    public void removeListener(ToneDetectorListener listener);
    public void clearAllListeners();
}
