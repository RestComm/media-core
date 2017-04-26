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

import java.io.InputStream;

import net.ripe.hadoop.pcap.packet.Packet;

/**
 * Execution context for a Play PCAP operation.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Ivelin Ivanov <ivelin.ivanov@telestax.com>
 *
 */
public class PcapPlayerContext {

    private PcapFile pcapFile;
    private InputStream pcapInputStream;

    /**
     * The timestamp in microseconds of the last pcap packet.<br>
     * We use this to ensure that packets are played at the same pace as they were recorded.
     */
    private long lastPacketTimestamp;
    /**
     * The playback timestamp in microseconds for the last played packet.<br>
     * This timestamp is matched to the pcap timestamp of the same packet in order to ensure consistent packet time spacing
     * between recording and playback.<br>
     * The initial value ensures sufficient distance to let the first packet through.
     */
    private long lastPacketPlaybackTimestamp;
    /**
     * While the current playback time distance between packets has not reached the recorded time distance, we suspend playback
     * temporarily.
     */
    private Packet suspendedPcapPacket;

    public PcapPlayerContext() {
        this.pcapFile = null;
        this.pcapInputStream = null;
        this.lastPacketTimestamp = 0;
        this.lastPacketPlaybackTimestamp = -1 * 0xFFFFFFFFL;
        this.suspendedPcapPacket = null;
    }

    public PcapFile getPcapFile() {
        return pcapFile;
    }

    public void setPcapFile(PcapFile pcapFile) {
        this.pcapFile = pcapFile;
    }

    public InputStream getPcapInputStream() {
        return pcapInputStream;
    }

    public void setPcapInputStream(InputStream pcapInputStream) {
        this.pcapInputStream = pcapInputStream;
    }

    public long getLastPacketTimestamp() {
        return lastPacketTimestamp;
    }

    public void setLastPacketTimestamp(long lastPacketTimestamp) {
        this.lastPacketTimestamp = lastPacketTimestamp;
    }

    public long getLastPacketPlaybackTimestamp() {
        return lastPacketPlaybackTimestamp;
    }

    public void setLastPacketPlaybackTimestamp(long lastPacketPlaybackTimestamp) {
        this.lastPacketPlaybackTimestamp = lastPacketPlaybackTimestamp;
    }

    public Packet getSuspendedPcapPacket() {
        return suspendedPcapPacket;
    }

    public void setSuspendedPcapPacket(Packet suspendedPcapPacket) {
        this.suspendedPcapPacket = suspendedPcapPacket;
    }

    public void reset() {
        this.pcapFile = null;
        this.pcapInputStream = null;
        this.lastPacketTimestamp = 0;
        this.lastPacketPlaybackTimestamp = -1 * 0xFFFFFFFFL;
        this.suspendedPcapPacket = null;
    }

}
