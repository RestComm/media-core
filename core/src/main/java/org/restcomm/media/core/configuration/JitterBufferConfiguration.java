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

package org.restcomm.media.core.configuration;

import org.restcomm.media.rtp.jitter.FixedJitterBuffer;
import org.restcomm.media.rtp.jitter.adaptive.strategy.LinearRecursiveFilter;

public class JitterBufferConfiguration {
    public static final int JITTER_BUFFER_SIZE = 50;
    public static final String JITTER_BUFFER_CLAZZ = FixedJitterBuffer.class.getName();
    public static final String PLAYOUT_STRATEGY_CLAZZ = LinearRecursiveFilter.class.getName();

    private int size = JITTER_BUFFER_SIZE;
    private String clazz = JITTER_BUFFER_CLAZZ;
    private String playoutStrategyClazz = PLAYOUT_STRATEGY_CLAZZ;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Jitter Buffer size must be positive.");
        }
        this.size = size;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getPlayoutStrategyClazz() {
        return playoutStrategyClazz;
    }

    public void setPlayoutStrategyClazz(String playoutStrategyClazz) {
        this.playoutStrategyClazz = playoutStrategyClazz;
    }

}
