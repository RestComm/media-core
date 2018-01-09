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

package org.restcomm.media.pcap;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class PcapPlayer {

    private static final Logger log = LogManager.getLogger(PcapPlayer.class);

    // Core Components
    private final ListeningScheduledExecutorService scheduler;

    // Network Components
    private final AsyncPcapChannel channel;

    // Execution Context
    private final AtomicBoolean playing;
    private final PcapPlayerContext context;

    public PcapPlayer(AsyncPcapChannel channel, ListeningScheduledExecutorService scheduler) {
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
        if (log.isDebugEnabled()) {
            log.debug("Scheduled PCAP packet playback for " + time + " " + unit.name());
        }

        ListenableScheduledFuture<Packet> future = this.scheduler.schedule(new PlayerWorker(), time, TimeUnit.MICROSECONDS);
        Futures.addCallback(future, new PlayerWorkerCallback(), this.scheduler);
    }

    public void play(URL filepath) throws IOException {
        if (this.playing.get()) {
            throw new IllegalStateException("PCAP Player is busy.");
        }
        
        // Reset execution context
        this.context.reset();

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
            scheduleRead(0L, TimeUnit.MICROSECONDS);
        }
    }

    public void stop() {
        if (this.playing.compareAndSet(true, false)) {
            // Close file
            try {
                this.context.getPcapFile().close();
            } catch (IOException e) {
                log.warn("Could not close PCAP file " + context.getPcapFile().toString(), e);
            }
            if (log.isDebugEnabled()) {
                log.debug("Stopped playing PCAP " + context.getPcapFile().getPath());
            }
        }
    }

    private final class PlayerWorker implements Callable<Packet> {

        private final ChannelSendCallback sendCallback;

        public PlayerWorker() {
            this.sendCallback = new ChannelSendCallback();
        }

        @Override
        public Packet call() {
            Packet packet = null;
            if (playing.get()) {
                // Send scheduled packet over the wire
                packet = context.getSuspendedPcapPacket();
                channel.send(packet, this.sendCallback);

                // Update statistics
                context.setSuspendedPcapPacket(null);
                context.setLastPacketPlaybackTimestamp((long) packet.get(Packet.TIMESTAMP) * 1000000L + (long) packet.get(Packet.TIMESTAMP_MICROS));
                context.setLastPacketTimestamp(System.nanoTime() / 1000L);
            }
            return packet;
        }

    }

    private final class PlayerWorkerCallback implements FutureCallback<Packet> {

        @Override
        public void onSuccess(Packet result) {
            if(result != null) {
                context.packetSent(result);
                if(log.isTraceEnabled()) {
                    int packetsSent = context.getPacketsSent();
                    int octetsSent = context.getOctetsSent();
                    URL pcapPath = context.getPcapFile().getPath();
                    log.info("PCAP Playback Statistics for "+ pcapPath.toString() +" [packets_sent = " + packetsSent + ", octets sent=" + octetsSent+"]");
                }
            }
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

        private void scheduleNextRead() {
            PcapFile pcap = context.getPcapFile();
            if (pcap.isComplete()) {
                // Stop playing if no more packets are available
                if(log.isDebugEnabled()) {
                    log.debug("Reached end of PCAP " + pcap.getPath().toString() + ". Player will stop.");
                }
                stop();
            } else {
                // Schedule next packet
                Packet nextPacket = pcap.read();
                context.setSuspendedPcapPacket(nextPacket);

                long nextPacketPlaybackTimestampSeconds = (long) nextPacket.get(Packet.TIMESTAMP);
                long nextPacketPlaybackTimestampMicros = (long) nextPacket.get(Packet.TIMESTAMP_MICROS);
                long nextPacketPlaybackTimestamp = nextPacketPlaybackTimestampSeconds * 1000000L + nextPacketPlaybackTimestampMicros;
                long nextPacketTimestamp = System.nanoTime() / 1000L;

                long timestampWindowframe = nextPacketTimestamp - context.getLastPacketTimestamp();
                long playbackWindowframe = nextPacketPlaybackTimestamp - context.getLastPacketPlaybackTimestamp();
                long suspensionTime = playbackWindowframe - timestampWindowframe;
                double latencyCompensation = suspensionTime * context.getLatencyCompensationFactor();
                suspensionTime -= latencyCompensation;

                scheduleRead(Math.max(0, suspensionTime), TimeUnit.MICROSECONDS);
            }
        }

        @Override
        public void onSuccess(Void result) {
            if (log.isTraceEnabled()) {
                log.trace("Sent PCAP RTP packet to remote peer");
            }
            if (playing.get()) {
                scheduleNextRead();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to send PCAP RTP packet to remote peer", t);
            }
            if (playing.get()) {
                scheduleNextRead();
            }
        }
    }

    public boolean isPlaying() {
        return this.playing.get();
    }
    
    public int countPacketsSent() {
        return context.getPacketsSent();
    }
    
    public int countOctetsSent() {
        return context.getOctetsSent();
    }

}
