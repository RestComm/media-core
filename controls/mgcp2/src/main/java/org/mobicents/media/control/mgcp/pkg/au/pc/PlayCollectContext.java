/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp.pkg.au.pc;

import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectContext {

    // Media Components
    private final DtmfDetector detector;
    private final DtmfDetectorListener detectorListener;
    
    // Signal options
    private final char endInputKey;
    
    // Runtime data
    private final StringBuilder collectedDigits;
    private long lastCollectedDigitOn;
    private int attempt;
    
    private int returnCode;
    
    public PlayCollectContext(DtmfDetector detector, DtmfDetectorListener detectorListener) {
        // Media Components
        this.detector = detector;
        this.detectorListener = detectorListener;
        
        // Signal Options
        this.endInputKey = '#';
        
        // Runtime Data
        this.collectedDigits = new StringBuilder("");
        this.lastCollectedDigitOn = 0L;
        this.returnCode = 0;
        this.attempt = 1;
    }
    
    /*
     * Media Components
     */
    public DtmfDetector getDetector() {
        return detector;
    }
    
    public DtmfDetectorListener getDetectorListener() {
        return detectorListener;
    }
    
    /*
     * Signal Options
     */
    public char getEndInputKey() {
        return endInputKey;
    }
    
    /*
     * Runtime Data
     */
    public void collectDigit(char digit) {
        this.collectedDigits.append(digit);
        this.lastCollectedDigitOn = System.currentTimeMillis();
    }
    
    public String getCollectedDigits() {
        return collectedDigits.toString();
    }
    
    public long getLastCollectedDigitOn() {
        return lastCollectedDigitOn;
    }
    
    public int getAttempt() {
        return attempt;
    }
    
    public int getReturnCode() {
        return returnCode;
    }
    
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

}
