/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.spi.resource;

import org.mobicents.media.MediaSink;

/**
 * 
 * @author amit bhayani
 * 
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
     * Specifies mask for dtmf sequence.
     * 
     * @return the mask assigned to detector as regular expression.
     */
    public String getMask();

    /**
     * Assign mask for DTMF sequence detection.
     * 
     * @param mask the regular expression string.
     */
    public void setMask(String mask);

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
    public void start();
    
    /**
     * Terminates media processing.
     */
    public void stop();
    
    public void flushBuffer();
    
}
