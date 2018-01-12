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

package org.restcomm.media.resource.speechdetector;

import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionPoint;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

/**
 * Components that detects user speech from a stream of incoming audio.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 *
 */
public abstract class SpeechDetector implements ExtensionPoint {

    private static final PluginManager pluginManager = new DefaultPluginManager();
    
    static {
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    /**
     * Detects whether the speech signal is present in passed sample buffer.
     * 
     * @param data buffer with samples
     * @param offset the position of first sample in buffer
     * @param len the number of samples
     * @return true if speech detected
     */
     public abstract boolean detect(byte[] data, int offset, int len);

    /**
     * Returns static Plugin Manager object.
     *
     * @return Plugin Manager
     */
      
     public static PluginManager getPluginManager() {
         return pluginManager;
     }
}
