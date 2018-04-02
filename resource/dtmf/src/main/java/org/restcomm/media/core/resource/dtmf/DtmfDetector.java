/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
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

package org.restcomm.media.core.resource.dtmf;

/**
 * Interface for DTMF detector component
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public interface DtmfDetector {

    /**
     * The method that detects DTMF digit in provided audio buffer. Detection
     * status is passed to the application layer through a listener pattern.
     *
     * @param data     buffer with samples
     * @param duration buffer duration 
     * @return Detected digit, null if nothing is detected
     */
    void detect(byte[] data, long duration);

    /**
     * DTMF tone volume in dBi
     *
     * @return Volume level
     */
    public int getDbi();

    /**
     * DTMF tone duration in milliseconds
     *
     * @return Tone duration
     */
    public int getToneDuration();

    /**
     * Interval between two DTMF tones in milliseconds
     *
     * @return Tone interval
     */
    public int getToneInterval();
}
