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

package org.mobicents.media.core.configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Configuration of Media elements.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaConfiguration {

    private int timeout;
    private int lowPort;
    private int highPort;
    private int jitterBufferSize;
    private final Set<String> codecs;

    public MediaConfiguration() {
        this.timeout = 0;
        this.lowPort = 34534;
        this.highPort = 65534;
        this.jitterBufferSize = 50;
        this.codecs = new HashSet<>(5);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Media timeout cannot be negative.");
        }
        this.timeout = timeout;
    }

    public int getLowPort() {
        return lowPort;
    }

    public void setLowPort(int lowPort) {
        if (lowPort < 0 || lowPort > 65534) {
            throw new IllegalArgumentException("Lowest media port must be positive and lower than 65535");
        } else if (lowPort > this.highPort) {
            throw new IllegalArgumentException("Lowest media port cannot be greater than highest port (" + this.highPort + ")");
        }
        this.lowPort = lowPort;
    }

    public int getHighPort() {
        return highPort;
    }

    public void setHighPort(int highPort) {
        if (highPort < 0 || highPort > 65534) {
            throw new IllegalArgumentException("Highest media port must be positive and lower than 65535");
        } else if (highPort < this.lowPort) {
            throw new IllegalArgumentException("Highest media port cannot be greater than lowest port (" + this.lowPort + ")");
        }
        this.highPort = highPort;
    }

    public int getJitterBufferSize() {
        return jitterBufferSize;
    }

    public void setJitterBufferSize(int jitterBufferSize) {
        if (jitterBufferSize < 0) {
            throw new IllegalArgumentException("Jitter Buffer size must be positive.");
        }
        this.jitterBufferSize = jitterBufferSize;
    }

    public void addCodec(String codec) {
        if (codec == null || codec.isEmpty()) {
            throw new IllegalArgumentException("Codec cannot be empty.");
        }
        this.codecs.add(codec.toLowerCase());
    }

    public Iterator<String> getCodecs() {
        return this.codecs.iterator();
    }
    
    public boolean hasCodec(String codec) {
        if(codec == null || codec.isEmpty()) {
            return false;
        }
        return this.codecs.contains(codec.toLowerCase());
    }
    
    public int countCodecs() {
        return this.codecs.size();
    }

    public void removeCodec(String codec) {
        this.codecs.remove(codec);
    }

    public void removeAllCodecs() {
        this.codecs.clear();
    }

}
