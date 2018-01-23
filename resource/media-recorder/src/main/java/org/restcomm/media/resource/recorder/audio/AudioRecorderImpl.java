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

package org.restcomm.media.resource.recorder.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.dtmf.DtmfTonesData;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;
import org.restcomm.media.spi.listener.Listeners;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.pooling.PooledObject;
import org.restcomm.media.spi.recorder.Recorder;
import org.restcomm.media.spi.recorder.RecorderEvent;
import org.restcomm.media.spi.recorder.RecorderListener;

/**
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Pavel Chlupacek (pchlupacek)
 */
public class AudioRecorderImpl extends AbstractSink implements Recorder, PooledObject {

    private static final long serialVersionUID = -5290778284867189598L;

    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();

    private final static int SILENCE_LEVEL = 10;

    static {
        formats.add(LINEAR);
    }

    private String recordDir;
    private AtomicReference<RecorderFileSink> sink = new AtomicReference<>(null);

    // if set ti true the record will terminate recording when silence detected
    private long postSpeechTimer = -1L;
    private long preSpeechTimer = -1L;

    // samples
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
    private byte[] data;
    private int offset;
    private int len;

    private KillRecording killRecording;
    private Heartbeat heartbeat;

    private long lastPacketData = 0, startTime = 0;

    private PriorityQueueScheduler scheduler;

    // maximum recrding time. -1 means until stopped.
    private long maxRecordTime = -1;

    // listener
    private Listeners<RecorderListener> listeners = new Listeners<RecorderListener>();

    // events
    private RecorderEventImpl recorderStarted;
    private RecorderEventImpl recorderStopped;
    private RecorderEventImpl recorderFailed;

    // event sender task
    private EventSender eventSender;

    // event qualifier
    private int qualifier;

    private boolean speechDetected = false;

    private AudioOutput output;
    private OOBOutput oobOutput;
    private OOBRecorder oobRecorder;

    private static final Logger logger = LogManager.getLogger(AudioRecorderImpl.class);

    public AudioRecorderImpl(PriorityQueueScheduler scheduler) {
        super("recorder");
        this.scheduler = scheduler;

        killRecording = new KillRecording();

        // initialize events
        recorderStarted = new RecorderEventImpl(RecorderEvent.START, this);
        recorderStopped = new RecorderEventImpl(RecorderEvent.STOP, this);
        recorderFailed = new RecorderEventImpl(RecorderEvent.FAILED, this);

        // initialize event sender task
        eventSender = new EventSender();
        heartbeat = new Heartbeat();

        output = new AudioOutput(scheduler, ComponentType.RECORDER.getType());
        output.join(this);

        oobOutput = new OOBOutput(scheduler, ComponentType.RECORDER.getType());
        oobRecorder = new OOBRecorder();
        oobOutput.join(oobRecorder);
    }

    public AudioOutput getAudioOutput() {
        return this.output;
    }

    public OOBOutput getOOBOutput() {
        return this.oobOutput;
    }

    @Override
    public void activate() {
        this.lastPacketData = scheduler.getClock().getTime();
        this.startTime = scheduler.getClock().getTime();

        output.start();
        oobOutput.start();
        if (this.postSpeechTimer > 0 || this.preSpeechTimer > 0 || this.maxRecordTime > 0) {
            scheduler.submitHeatbeat(this.heartbeat);
        }

        // send event
        fireEvent(recorderStarted);
    }

    @Override
    public void deactivate() {
        if (!this.isStarted()) {
            return;
        }

        try {
            output.stop();
            oobOutput.stop();
            this.maxRecordTime = -1;
            this.lastPacketData = 0;
            this.startTime = 0;

            this.heartbeat.cancel();

            // deactivate can be concurrently invoked from  multiple threads (MediaGroup, KillRecording for example).
            // to make sure the sink is closed only once, we set the sink ref to null and proceed to commit only if obtained reference is not null.

            RecorderFileSink snk = sink.getAndSet(null);
            if (snk != null) {
                snk.commit();
            }
        } catch (Exception e) {
            logger.error("Error writing to file", e);
        } finally {
            // send event
            recorderStopped.setQualifier(qualifier);
            fireEvent(recorderStopped);

            // clean qualifier
            this.qualifier = 0;
            this.maxRecordTime = -1L;
            this.postSpeechTimer = -1L;
            this.preSpeechTimer = -1L;
            this.speechDetected = false;
        }
    }

    @Override
    public void setPreSpeechTimer(long value) {
        this.preSpeechTimer = value;
    }

    @Override
    public void setPostSpeechTimer(long value) {
        this.postSpeechTimer = value;
    }

    @Override
    public void setMaxRecordTime(long maxRecordTime) {
        this.maxRecordTime = maxRecordTime;
    }

    /**
     * Fires specified event
     * 
     * @param event the event to fire.
     */
    private void fireEvent(RecorderEventImpl event) {
        eventSender.event = event;
        scheduler.submit(eventSender, PriorityQueueScheduler.INPUT_QUEUE);
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        // extract data
        data = frame.getData();
        offset = frame.getOffset();
        len = frame.getLength();

        byteBuffer.clear();
        byteBuffer.limit(len - offset);
        byteBuffer.put(data, offset, len - offset);
        byteBuffer.rewind();
        RecorderFileSink snk = sink.get();
        if (snk != null) snk.write(byteBuffer);

        if (this.postSpeechTimer > 0 || this.preSpeechTimer > 0) {
            // detecting silence
            if (!this.checkForSilence(data, offset, len)) {
                this.lastPacketData = scheduler.getClock().getTime();
                if(!this.speechDetected) {
                    fireEvent(new RecorderEventImpl(RecorderEvent.SPEECH_DETECTED, this));
                }
                this.speechDetected = true;
            }
        } else {
            this.lastPacketData = scheduler.getClock().getTime();
        }
    }

    @Override
    public void setRecordDir(String recordDir) {
        this.recordDir = recordDir;
    }

    @Override
    public void setRecordFile(String uri, boolean append) throws IOException {
        // calculate the full path
        String path = uri.startsWith("file:") ? uri.replaceAll("file://", "") : this.recordDir + "/" + uri;
        Path file = Paths.get(path);

        RecorderFileSink snk = sink.getAndSet(new RecorderFileSink(file,append));
        if (snk != null) {
            logger.error("Sink for the recording is not cleaned properly, found " + snk);
        }
    }

    /**
     * Checks does the frame contains sound or silence.
     * 
     * @param data buffer with samples
     * @param offset the position of first sample in buffer
     * @param len the number if samples
     * @return true if silence detected
     */
    private boolean checkForSilence(byte[] data, int offset, int len) {
        int[] correllation = new int[len];
        for (int i = offset; i < len - 1; i += 2) {
            correllation[i] = (data[i] & 0xff) | (data[i + 1] << 8);
        }

        double mean = mean(correllation);
        if(mean > SILENCE_LEVEL) {
            return false;
        }
        return true;
    }
    
    public double mean(int[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    @Override
    public void addListener(RecorderListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    @Override
    public void removeListener(RecorderListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearAllListeners() {
        listeners.clear();
    }
    
    @Override
    public void checkIn() {
        // clear listeners
        clearAllListeners();

        // clean buffers
        this.byteBuffer.clear();
        this.data = null;
        this.offset = 0;
        this.len = 0;
        
        // reset internal state
        this.recordDir = "";
        this.postSpeechTimer = -1L;
        this.preSpeechTimer = -1L;
        this.lastPacketData = 0L;
        this.startTime = 0L;
        this.maxRecordTime = -1L;
        this.qualifier = 0;
        this.speechDetected = false;
    }

    @Override
    public void checkOut() {
        // TODO Auto-generated method stub
    }

    /**
     * Asynchronous recorder stopper.
     */
    private class KillRecording extends Task {

        public KillRecording() {
            super();
        }

        @Override
        public long perform() {
            deactivate();
            return 0;
        }

        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }
    }

    /**
     * Asynchronous recorder stopper.
     */
    private class EventSender extends Task {

        protected RecorderEventImpl event;

        public EventSender() {
            super();
        }

        @Override
        public long perform() {
            listeners.dispatch(event);
            return 0;
        }

        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }
    }

    /**
     * Heartbeat
     */
    private class Heartbeat extends Task {

        public Heartbeat() {
            super();
        }

        @Override
        public long perform() {
            final long currentTime = scheduler.getClock().getTime();
            final long idleTime = currentTime - lastPacketData;

            // Abort recording operation if user did not speak during initial detection period
            if (preSpeechTimer > 0 && !speechDetected && idleTime > preSpeechTimer) {
                qualifier = RecorderEvent.NO_SPEECH;
                scheduler.submit(killRecording, PriorityQueueScheduler.INPUT_QUEUE);
                return 0;
            }

            // Abort recording operation if user did not speak for a while
            if (postSpeechTimer > 0 && speechDetected && idleTime > postSpeechTimer) {
                qualifier = RecorderEvent.SUCCESS;
                scheduler.submit(killRecording, PriorityQueueScheduler.INPUT_QUEUE);
                return 0;
            }

            // Abort recording if maximum time limit is reached
            final long duration = currentTime - startTime;
            if (maxRecordTime > 0 && duration >= maxRecordTime) {
                qualifier = RecorderEvent.MAX_DURATION_EXCEEDED;
                scheduler.submit(killRecording, PriorityQueueScheduler.INPUT_QUEUE);
                return 0;
            }

            scheduler.submitHeatbeat(this);
            return 0;
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.HEARTBEAT_QUEUE;
        }

    }

    private class OOBRecorder extends AbstractSink {

        private static final long serialVersionUID = -7570027234464617359L;

        private byte currTone = (byte) 0xFF;
        private long latestSeq = 0;

        private boolean hasEndOfEvent = false;
        private long endSeq = 0;

        private ByteBuffer toneBuffer = ByteBuffer.allocateDirect(1600);

        public OOBRecorder() {
            super("oob recorder");
        }

        @Override
        public void onMediaTransfer(Frame buffer) throws IOException {
            byte[] data = buffer.getData();
            if (data.length != 4) {
                return;
            }

            boolean endOfEvent = false;
            endOfEvent = (data[1] & 0X80) != 0;

            // lets ignore end of event packets
            if (endOfEvent) {
                hasEndOfEvent = true;
                endSeq = buffer.getSequenceNumber();
                return;
            }

            // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
            if (currTone == data[0]) {
                if (hasEndOfEvent) {
                    if (buffer.getSequenceNumber() <= endSeq && buffer.getSequenceNumber() > (endSeq - 8)) {
                        // out of order , belongs to same event
                        // if comes after end of event then its new one
                        return;
                    }
                } else if ((buffer.getSequenceNumber() < (latestSeq + 8)) && buffer.getSequenceNumber() > (latestSeq - 8)) {
                    if (buffer.getSequenceNumber() > latestSeq) {
                        latestSeq = buffer.getSequenceNumber();
                    }
                    return;
                }
            }

            hasEndOfEvent = false;
            endSeq = 0;

            latestSeq = buffer.getSequenceNumber();
            currTone = data[0];
            toneBuffer.clear();
            toneBuffer.limit(DtmfTonesData.buffer[data[0]].length);
            toneBuffer.put(DtmfTonesData.buffer[data[0]]);
            toneBuffer.rewind();
            RecorderFileSink snk = sink.get();
            if (snk != null) snk.write(toneBuffer);
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

}
