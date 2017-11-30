/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.drivers.asr;

import java.util.List;
import java.util.Map;

/**
 * Driver that establishes and manages connection with ASR Service.
 * 
 * @author gdubina
 */
public interface AsrDriver {

    /**
     * Configures the driver.
     * 
     * @param parameters The set of configuration parameters.
     */
    void configure(Map<String, String> parameters);

    /**
     * Starts the speech recognition process.
     * 
     * @param lang The language code as defined by BCP-47.
     * @param hints A list of words and sentences to help improve the detection accuracy.
     */
    void startRecognizing(String lang, List<String> hints);

    /**
     * Stops the speech recognition process.
     */
    void finishRecognizing();

    /**
     * Provide audio to the ASR engine.
     * 
     * @param data The audio payload.
     * @param offset The payload offset.
     * @param len The length of data to be passed.
     */
    void write(byte[] data, int offset, int len);

    /**
     * Provide audio to the ASR engine.
     * 
     * @param data The audio payload.
     */
    void write(byte[] data);

    /**
     * Attaches a listener to the driver.
     * 
     * @param listener The listener to be attached to the driver.
     */
    void setListener(AsrDriverEventListener listener);

    /**
     * Gets the amount of time the driver will wait for the provider to reply with a transcription.
     * 
     * @return The response time in milliseconds.
     */
    int getResponseTimeoutInMilliseconds();

}