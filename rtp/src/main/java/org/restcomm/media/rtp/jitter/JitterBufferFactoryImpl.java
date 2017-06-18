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

package org.restcomm.media.rtp.jitter;

import org.restcomm.media.rtp.jitter.adaptive.AdaptiveJitterBuffer;
import org.restcomm.media.rtp.jitter.adaptive.strategy.PlayoutStrategy;

public class JitterBufferFactoryImpl implements JitterBufferFactory {
    private final int size;
    private final String clazz;
    private final String playoutStrategyClazz;

    public JitterBufferFactoryImpl(int size, String clazz, String playoutStrategyClazz) {
        super();
        this.size = size;
        this.clazz = clazz;
        this.playoutStrategyClazz = playoutStrategyClazz;
    }

    @Override
    public JitterBuffer getJitterBuffer() {
        JitterBuffer jitterBuffer = null;
        ClassLoader loader = JitterBufferFactoryImpl.class.getClassLoader();
        try {
            jitterBuffer = (JitterBuffer) loader.loadClass(clazz).newInstance();
            jitterBuffer.setJitterbufferSize(size);
            if (playoutStrategyClazz != null && jitterBuffer instanceof AdaptiveJitterBuffer) {
                ((AdaptiveJitterBuffer) jitterBuffer)
                        .setPlayoutStrategy((PlayoutStrategy) loader.loadClass(clazz).newInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jitterBuffer;
    }

}
