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

package org.restcomm.media.bootstrap.ioc.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderImpl;
import org.restcomm.media.resource.speechdetector.SpeechDetectorProvider;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.pooling.PooledObjectFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioRecorderFactoryProvider implements Provider<AudioRecorderFactory> {

    private final PriorityQueueScheduler mediaScheduler;
    private final SpeechDetectorProvider speechDetectorProvider;

    @Inject
    public AudioRecorderFactoryProvider(PriorityQueueScheduler mediaScheduler, SpeechDetectorProvider speechDetectorProvider) {
        this.mediaScheduler = mediaScheduler;
        this.speechDetectorProvider = speechDetectorProvider;
    }

    @Override
    public AudioRecorderFactory get() {
        return new AudioRecorderFactory(mediaScheduler, this.speechDetectorProvider);
    }

    public static final class AudioRecorderFactoryType extends TypeLiteral<PooledObjectFactory<AudioRecorderImpl>> {

        public static final AudioRecorderFactoryType INSTANCE = new AudioRecorderFactoryType();

        private AudioRecorderFactoryType() {
            super();
        }

    }

}
