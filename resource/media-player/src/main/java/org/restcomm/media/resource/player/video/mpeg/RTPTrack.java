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

package org.restcomm.media.resource.player.video.mpeg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class RTPTrack {

    private final Logger logger = LogManager.getLogger(this.getClass());
    
    private List<RTPLocalPacket> rtPktList = new ArrayList<RTPLocalPacket>();
    
    private TrackBox hintTrackBox = null;
    private TrackBox trackBox = null;
    
    private RandomAccessFile hintTrackRAF = null;
    private RandomAccessFile trackRAF = null;
    
    private long[] hintSamplesOffSet = null;
    private long[] samplesOffSet = null;
    private long[] sampleDelta = null;
    
    private int period = 0;
    private int[] timeDelta = null;
    
    private volatile int hintSamplesSent = 0;
    private volatile int audioSamplesSent = 0;
    private volatile long rtpTimeStamp = Math.abs((new Random()).nextInt());
    
    private String sdpText;
    private long trackId = -1;
    
    long hintSampleCount = 0;
    double npt;
    boolean first = true;

    public RTPTrack(TrackBox audioTrackBox, TrackBox audioHintTrackBox, File file) throws FileNotFoundException {

        long sampleCount = 0;
        SampleTableBox hintSampleTableBox = null;
        SampleTableBox sampleTableBox = null;
        long[] hintChunkOffset = null;
        long[] hintSamplesPerChunk = null;
        long[] hintEntrySize = null;
        long hintSampleSize = 0;
        int timeScale = 0;

        
        this.trackBox = audioTrackBox;
        this.hintTrackBox = audioHintTrackBox;
        this.hintTrackRAF = new RandomAccessFile(file, "r");
        this.trackRAF = new RandomAccessFile(file, "r");

        npt = (this.hintTrackBox.getMediaBox().getMediaHeaderBox().getDuration()) / (this.hintTrackBox.getMediaBox().getMediaHeaderBox().getTimescale());

        // TODO : We are assuming that Hint Track is referencing only one Track, but there could be more.
        trackId = this.hintTrackBox.getTrackHeaderBox().getTrackID();

        for (Box b : this.hintTrackBox.getUserDataBox().getUserDefinedBoxes()) {
            if (b.getType().equals(TrackHintInformation.TYPE_S)) {
                this.sdpText = ((TrackHintInformation) b).getRtpTrackSdpHintInformation().getSdpText();
            }
        }

        hintSampleTableBox = this.hintTrackBox.getMediaBox().getMediaInformationBox().getSampleTableBox();
        sampleTableBox = this.trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox();

        hintChunkOffset = hintSampleTableBox.getChunkOffsetBox().getChunkOffset();
        long[] audioChunkOffset = sampleTableBox.getChunkOffsetBox().getChunkOffset();

        if (logger.isDebugEnabled()) {
            logger.debug("Hint ChunkOffset length = " + hintChunkOffset.length);
            logger.debug("ChunkOffset length = " + audioChunkOffset.length);
        }

        for (SampleEntry sampleEntry : hintSampleTableBox.getSampleDescription().getSampleEntries()) {
            if (sampleEntry.getType().equals(RtpHintSampleEntry.TYPE_S)) {
                for (Box box : ((RtpHintSampleEntry) sampleEntry).getAdditionaldata()) {
                    if (box.getType().equals(TimeScaleEntry.TYPE_S)) {
                        timeScale = ((TimeScaleEntry) box).getTimeScale();
                        if (logger.isDebugEnabled()) {
                            logger.debug("timeScale = " + timeScale);
                        }

                    }
                }
            }
        }
        hintSampleCount = hintSampleTableBox.getSampleSizeBox().getSampleCount();
        sampleCount = sampleTableBox.getSampleSizeBox().getSampleCount();
        TimeToSampleBox timeToSampleBox = hintSampleTableBox.getTimeToSampleBox();
        if (timeToSampleBox.getEntryCount() == 1) {
            // this.heartBeat = (int) (this.duration / this.timeScale * 1000) / this.sampleCount;
            this.period = (int) (((float) timeToSampleBox.getSampleDelta()[0] / (float) timeScale) * 1000);

        } else {
            // heart beat different for each sample
            int heartBeatArrCount = 0;
            this.timeDelta = new int[(int) hintSampleCount];
            long[] sampleCountArr = timeToSampleBox.getSampleCount();
            long[] sampleDelta = timeToSampleBox.getSampleDelta();
            for (int i = 0; i < sampleCountArr.length; i++) {
                long temp = sampleCountArr[i];
                timeDelta[heartBeatArrCount++] = (int) (((float) sampleDelta[i] / (float) timeScale) * 1000);
                for (int j = 1; j < temp; j++) {
                    timeDelta[heartBeatArrCount++] = (int) sampleDelta[i];
                }
            }
        }

        this.hintSamplesOffSet = new long[(int) hintSampleCount];
        this.samplesOffSet = new long[(int) sampleCount];

        if (logger.isDebugEnabled()) {
            logger.debug("Heart Beat = " + this.period);
            if (this.period == 0) {
                logger.debug("heartBeat zero so use heartBeatArr. Length = " + timeDelta.length);
            }
        }

        hintSamplesPerChunk = new long[hintChunkOffset.length];
        long[] audioSamplesPerChunk = new long[audioChunkOffset.length];

        // Calculate the Number of Samples for each Chunk for Hint Audio Track
        int samplesPerChunkCount = 0;

        long[] hintFirstChunk = hintSampleTableBox.getSampleToChunkBox().getFirstChunk();
        long[] hintSamplesPerChunkTemp = hintSampleTableBox.getSampleToChunkBox().getSamplesPerChunk();

        long samplesAtChunk;
        for (int i = 0; i < (hintFirstChunk.length - 1); i++) {
            long temp = (hintFirstChunk[i + 1] - hintFirstChunk[i]);
            samplesAtChunk = hintSamplesPerChunkTemp[i];

            for (int j = 0; j < temp; j++) {
                hintSamplesPerChunk[samplesPerChunkCount++] = samplesAtChunk;
            }
        }

        samplesAtChunk = hintSamplesPerChunkTemp[(hintFirstChunk.length - 1)];
        for (int j = samplesPerChunkCount; j < hintSamplesPerChunk.length; j++) {
            hintSamplesPerChunk[samplesPerChunkCount++] = samplesAtChunk;
        }

        // Calculate the Number of Samples for each Chunk for Audio Track
        samplesPerChunkCount = 0;
        long[] audioFirstChunk = sampleTableBox.getSampleToChunkBox().getFirstChunk();
        long[] audioSamplesPerChunkTemp = sampleTableBox.getSampleToChunkBox().getSamplesPerChunk();

        for (int i = 0; i < (audioFirstChunk.length - 1); i++) {
            long temp = (audioFirstChunk[i + 1] - audioFirstChunk[i]);
            samplesAtChunk = audioSamplesPerChunkTemp[i];

            for (int j = 0; j < temp; j++) {
                audioSamplesPerChunk[samplesPerChunkCount++] = samplesAtChunk;
            }
        }

        samplesAtChunk = audioSamplesPerChunkTemp[(audioFirstChunk.length - 1)];
        for (int j = samplesPerChunkCount; j < audioSamplesPerChunk.length; j++) {
            audioSamplesPerChunk[samplesPerChunkCount++] = samplesAtChunk;
        }

        // This is debug
        if (logger.isDebugEnabled()) {
            int tempCnt = 0;
            for (int i = 0; i < hintSamplesPerChunk.length; i++) {
                tempCnt += hintSamplesPerChunk[i];
            }

            logger.debug("Total sample count for Hint Track that should match with SampleSizeBox sampleCount(" + hintSampleCount + ") = " + tempCnt);

            tempCnt = 0;
            for (int i = 0; i < audioSamplesPerChunk.length; i++) {
                tempCnt += audioSamplesPerChunk[i];
            }

            logger.debug("Total sample count for Track that should match with SampleSizeBox sampleCount(" + sampleTableBox.getSampleSizeBox().getSampleCount() + ") = " + tempCnt);
        }// Debug ends

        // Calculate the OffSet for each Sample for Hint Track
        hintSampleSize = hintSampleTableBox.getSampleSizeBox().getSampleSize();
        hintEntrySize = hintSampleTableBox.getSampleSizeBox().getEntrySize();
        int samplesOffSetCount = 0;
        for (int i = 0; i < hintChunkOffset.length; i++) {
            long chunkOffForChunk = hintChunkOffset[i];
            long samplesInThisChunk = hintSamplesPerChunk[i];

            hintSamplesOffSet[samplesOffSetCount++] = chunkOffForChunk;

            for (int j = 1; j < samplesInThisChunk; j++) {
                if (hintSampleSize == 0) {
                    hintSamplesOffSet[samplesOffSetCount++] = hintSamplesOffSet[samplesOffSetCount - 2] + hintEntrySize[samplesOffSetCount - 2];
                } else {
                    hintSamplesOffSet[samplesOffSetCount++] = (chunkOffForChunk + hintSampleSize * j);
                }
            }

        }

        // debug
        // if (logger.isDebugEnabled()) {
        // logger.info("Each Sample Off Set for Hint Track ");
        // StringBuffer b = new StringBuffer();
        // for (int i = 0; i < hintSamplesOffSet.length; i++) {
        // b.append(hintSamplesOffSet[i]).append(",");
        // }
        // logger.debug(b.toString());
        // }// debug ends

        // Calculate the OffSet for each Sample for Audio Track
        long audioSampleSize = sampleTableBox.getSampleSizeBox().getSampleSize();
        long[] audioEntrySize = sampleTableBox.getSampleSizeBox().getEntrySize();
        samplesOffSetCount = 0;
        for (int i = 0; i < audioChunkOffset.length; i++) {
            long chunkOffForChunk = audioChunkOffset[i];
            long samplesInThisChunk = audioSamplesPerChunk[i];

            samplesOffSet[samplesOffSetCount++] = chunkOffForChunk;

            for (int j = 1; j < samplesInThisChunk; j++) {
                if (audioSampleSize == 0) {
                    samplesOffSet[samplesOffSetCount++] = samplesOffSet[samplesOffSetCount - 2] + audioEntrySize[samplesOffSetCount - 2];
                } else {
                    samplesOffSet[samplesOffSetCount++] = (chunkOffForChunk + audioSampleSize * j);
                }
            }

        }

        // debug. Let us keep it till 3000 only else it will eat away all memory
        // if (logger.isDebugEnabled() && (samplesOffSet.length < 3000)) {
        // logger.info("Each Sample Off Set for Track ");
        // StringBuffer b = new StringBuffer();
        // for (int i = 0; i < this.samplesOffSet.length; i++) {
        // b.append(this.samplesOffSet[i]).append(",");
        // }
        // logger.debug(b.toString());
        // }// debug ends

        // Calculate SampleDelta for each sample
        sampleDelta = new long[(int) hintSampleCount];

        long[] hintSampleCountArr = hintSampleTableBox.getTimeToSampleBox().getSampleCount();
        long[] hintSampleDeltaArr = hintSampleTableBox.getTimeToSampleBox().getSampleDelta();
        samplesOffSetCount = 0;
        for (int i = 0; i < hintSampleCountArr.length; i++) {
            long smplCnt = hintSampleCountArr[i];
            long smplDelts = hintSampleDeltaArr[i];
            for (int j = 0; j < (int) smplCnt; j++) {
                sampleDelta[samplesOffSetCount++] = smplDelts;
            }
        }

    }

    public void setRtpTime(long rtpTime) {
        this.rtpTimeStamp = rtpTime;
    }

    public RTPSample process() throws IOException {

        RTPSample rtpSample = null;

        if (this.hintSampleCount == this.hintSamplesSent) {
            return null;
        }

        rtpSample = new RTPSample();

        RTPLocalPacket rtpPacket = null;
        long hintSampleOffset = hintSamplesOffSet[hintSamplesSent];
        long audioSampleOffSet = samplesOffSet[audioSamplesSent];

        this.hintTrackRAF.seek(hintSampleOffset);

        int packetCount = (this.hintTrackRAF.read() << 8 | this.hintTrackRAF.read());

        rtpSample.setPacketCount(packetCount);

        if (packetCount > 0) {
            this.audioSamplesSent++;
        }

        // reserved
        this.hintTrackRAF.skipBytes(2);

        if (first) {
            first = false;
        } else {
            rtpTimeStamp += sampleDelta[hintSamplesSent];
        }

        for (int i = 0; i < packetCount; i++) {
            rtpPacket = new RTPLocalPacket();
            rtpPacket.load(this.hintTrackRAF);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Now load the Payload
            for (RTPConstructor rtpCons : rtpPacket.RTPConstructorList()) {
                switch (rtpCons.getConstructorType()) {
                    case RTPNoOpConstructor.TYPE:
                        // TODO : Anything to do here?
                        break;
                    case RTPImmediateConstructor.TYPE:
                        byte[] data = ((RTPImmediateConstructor) rtpCons).getData();
                        bos.write(data, 0, data.length);
                        break;
                    case RTPSampleConstructor.TYPE:
                        RTPSampleConstructor rtpSampCons = (RTPSampleConstructor) rtpCons;

                        // TODO : Can we avoid creating the byte[] again here?

                        byte[] rtpPaylod = null;

                        // TODO : Verify if this is correct?

                        /*
                         * From page 76 of ISO/IEC 14496-12
                         * 
                         * For hint tracks where the media is sent �?in the clear’, the sample entry then specifies the
                         * bytes to copy from the media track, by giving the sample number, data offset, and length to copy.
                         * The track reference may index into the table of track references (a strictly positive value),
                         * name the hint track itself (-1), or the only associated media track (0). (The value zero is
                         * therefore equivalent to the value 1.)
                         * 
                         * I couldn't make if bellow is what they are saying? Why do they talk so cryptic?
                         */
                        if (rtpSampCons.getTrackRefIndex() == -1) {
                            this.hintTrackRAF.seek((hintSampleOffset + rtpSampCons.getSampleOffSet()));
                            rtpPaylod = new byte[rtpSampCons.getLength()];
                            this.hintTrackRAF.read(rtpPaylod, 0, rtpSampCons.getLength());
                        } else {
                            // TODO : Should OffSet be added even if we are referring to non rtp mdat?
                            this.trackRAF.seek(audioSampleOffSet + rtpSampCons.getSampleOffSet());
                            rtpPaylod = new byte[rtpSampCons.getLength()];
                            int read = this.trackRAF.read(rtpPaylod, 0, rtpSampCons.getLength());
                        }

                        bos.write(rtpPaylod, 0, rtpPaylod.length);

                        break;
                    case RTPSampleDescriptionConstructor.TYPE:
                        // TODO : What here?
                        break;
                }
            }
            rtpPacket.setPayload(bos.toByteArray());
            rtpPacket.setRtpTimestamp(this.rtpTimeStamp);

            rtpSample.addRtpLocalPackets(rtpPacket);

        }// for

        if (this.period == 0.0) {
            rtpSample.setSamplePeriod(this.timeDelta[hintSamplesSent]);
        } else {
            rtpSample.setSamplePeriod(this.period);
        }

        hintSamplesSent++;

        // FIXME Do we care for extraByte?

        return rtpSample;

    }

    public void close() {
        try {
            if (this.hintTrackRAF != null) {
                this.hintTrackRAF.close();
            }

            if (this.trackRAF != null) {
                this.trackRAF.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.hintTrackRAF = null;
        this.trackRAF = null;

        this.hintTrackBox = null;
        this.trackBox = null;

        this.hintSamplesOffSet = null;
        this.samplesOffSet = null;

        this.timeDelta = null;

    }

    public String getSdpText() {
        return sdpText;
    }

    public long getTrackId() {
        return this.trackId;
    }

    public float getPacketPeriod() {
        return this.period;
    }

    public float getHeartBeat() {
        return this.period;
    }

    public double getNPT() {
        return this.npt;
    }
}
