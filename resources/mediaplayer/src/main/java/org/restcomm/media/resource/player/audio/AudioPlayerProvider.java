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

package org.restcomm.media.resource.player.audio;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dsp.DspFactory;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerProvider;

/**
 * Provides audio players.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioPlayerProvider implements PlayerProvider {

    private static final Logger log = LogManager.getLogger(AudioPlayerProvider.class);

    private final PriorityQueueScheduler scheduler;
    private final RemoteStreamProvider remoteStreamProvider;
    private final AtomicInteger id;
    private DspFactory dsp;

    public AudioPlayerProvider(PriorityQueueScheduler scheduler, RemoteStreamProvider remoteStreamProvider, DspFactory dsp) {
        this.scheduler = scheduler;
        this.remoteStreamProvider = remoteStreamProvider;
        this.dsp = dsp;
        this.id = new AtomicInteger(0);
    }

    public Player provide() {
        AudioPlayerImpl player = new AudioPlayerImpl(nextId(), this.scheduler, remoteStreamProvider);
        try {
            player.setDsp(this.dsp.newProcessor());
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            log.warn("Could not set DSP for newly provided Audio Player. Transcoding wont be possible!");
        }
        return player;
    }

    private String nextId() {
        return "audio-player" + id.getAndIncrement();
    }

}
