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

package org.restcomm.media.rtp.pcap;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import net.ripe.hadoop.pcap.packet.Packet;

/**
 * 
 * Media Player that reads RTP packets from a PCAP file and sends them to a remote peer.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Ivelin Ivanov <ivelin.ivanov@telestax.com>
 *
 */
public class PcapRtpPlayer {

    private static final Logger log = Logger.getLogger(PcapRtpPlayer.class);

    // Core Components
    private final ListeningScheduledExecutorService scheduler;

    // Network Components
    private final AsyncPcapChannel channel;

    // Execution Context
    private final AtomicBoolean playing;
    private final PcapPlayerContext context;
    private Future<?> playerFuture;

    public PcapRtpPlayer(AsyncPcapChannel channel, ListeningScheduledExecutorService scheduler) {
        // Core Components
        this.scheduler = scheduler;

        // Network Components
        this.channel = channel;

        // Execution Context
        this.playing = new AtomicBoolean(false);
        this.context = new PcapPlayerContext();
    }

    private PcapFile loadFile(URL filepath) throws IOException {
        PcapFile pcapFile = new PcapFile(filepath);
        try {
            pcapFile.open();
        } catch (IOException e) {
            try {
                pcapFile.close();
            } catch (IOException e2) {
                log.warn("Could not close PCAP file " + filepath + " elegantly.", e2);
            }
            throw e;
        }
        return pcapFile;
    }

    private void scheduleRead(long time, TimeUnit unit) {
        ListenableScheduledFuture<Long> future = this.scheduler.schedule(new PlayerWorker(), time, unit);
        Futures.addCallback(future, new PlayerWorkerCallback(), this.scheduler);
    }

    public void play(URL filepath) throws IOException {
        if (this.playing.get()) {
            throw new IllegalStateException("PCAP Player is busy.");
        }

        // Load pcap and store it in context
        PcapFile pcap = loadFile(filepath);
        this.context.setPcapFile(pcap);

        // Start reading operation
        this.playing.set(true);

        // Read first packet and play it
        if (pcap.isComplete()) {
            stop();
        } else {
            Packet packet = pcap.read();
            this.context.setSuspendedPcapPacket(packet);
            ListenableScheduledFuture<Long> future = this.scheduler.schedule(new PlayerWorker(), 0L, TimeUnit.MICROSECONDS);
            Futures.addCallback(future, new PlayerWorkerCallback(), this.scheduler);
        }
    }

    public void stop() {
        if (this.playing.compareAndSet(true, false)) {
            // Stop reading from file
            if (this.playerFuture != null) {
                this.playerFuture.cancel(false);
            }
            // Close file
            try {
                this.context.getPcapFile().close();
            } catch (IOException e) {
                log.warn("Could not close PCAP file " + context.getPcapFile().toString(), e);
            }
        }
    }

    private final class PlayerWorker implements Callable<Long> {

        private final ChannelSendCallback sendCallback;

        public PlayerWorker() {
            this.sendCallback = new ChannelSendCallback();
        }

        @Override
        public Long call() {
            long suspensionTime = -1L;
            if (playing.get()) {
                PcapFile pcap = context.getPcapFile();

                // Send scheduled packet over the wire
                Packet packet = context.getSuspendedPcapPacket();
                channel.send(packet, this.sendCallback);

                // Update statistics
                context.setSuspendedPcapPacket(null);
                context.setLastPacketTimestamp((long) packet.get(Packet.TIMESTAMP));
                context.setLastPacketPlaybackTimestamp((long) packet.get(Packet.TIMESTAMP_MICROS));

                if (pcap.isComplete()) {
                    // Stop playing if no more packets are available
                    stop();
                } else {
                    // Schedule next packet
                    Packet nextPacket = pcap.read();
                    context.setSuspendedPcapPacket(nextPacket);

                    long nowMicros = System.nanoTime() / 1000;
                    long nextPacketTimestampSeconds = (long) nextPacket.get(Packet.TIMESTAMP);
                    long nextPacketTimestampMicros = (long) nextPacket.get(Packet.TIMESTAMP_MICROS);
                    long nextPacketTimestamp = nextPacketTimestampSeconds * 1000000L + nextPacketTimestampMicros;
                    suspensionTime = ((nextPacketTimestamp - context.getLastPacketTimestamp())
                            - (nowMicros - context.getLastPacketPlaybackTimestamp()));

                    if (log.isDebugEnabled()) {
                        log.debug("Suspending PCAP packet playback for " + suspensionTime
                                + " microseconds in order to simulate recorded pcap packet timestamp difference.");
                    }
                }
            }
            return suspensionTime;
        }

    }

    private final class PlayerWorkerCallback implements FutureCallback<Long> {

        @Override
        public void onSuccess(Long result) {
            scheduleRead(result, TimeUnit.MICROSECONDS);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Could not play PCAP frame. Aborting play operation.", t);
            if (playing.get()) {
                stop();
            }
        }
    }

    private final class ChannelSendCallback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            if (log.isTraceEnabled()) {
                log.trace("Sent PCAP RTP packet to remote peer");
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to send PCAP RTP packet to remote peer", t);
            }
        }
    }

}
