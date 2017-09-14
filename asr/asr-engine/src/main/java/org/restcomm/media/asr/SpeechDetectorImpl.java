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

package org.restcomm.media.asr;

import java.io.IOException;

import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.spi.memory.Frame;

/**
 * @author anikiforov
 *
 */
public class SpeechDetectorImpl extends AbstractSink implements SpeechDetector {

    private static final long serialVersionUID = -1624065373751664278L;

    private SpeechDetectorListener listener;
    private boolean speechDetectionOn;
    private final int silenceLevel;

    public SpeechDetectorImpl(String name, final int silenceLevel) {
        super(name);
        this.silenceLevel = silenceLevel;
        this.speechDetectionOn = false;
    }

    public boolean checkForSilence(final Frame frame) {
        // extract data
        final byte[] data = frame.getData();
        final int offset = frame.getOffset();
        final int len = frame.getLength();
        return checkForSilence(data, offset, len);
    }

    @Override
    public void startSpeechDetection(final SpeechDetectorListener listener) {
        this.listener = listener;
        speechDetectionOn = true;
    }

    @Override
    public void stopSpeechDetection() {
        listener = null;
        speechDetectionOn = false;
    }

    protected boolean isSpeechDetectionOn() {
        return speechDetectionOn;
    }

    /*
     * Implementation of AbstractSink class abstract methods
     */

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        // detecting silence
        if (isSpeechDetectionOn() && (listener != null) && !checkForSilence(frame)) {
            listener.onSpeechDetected();
        }
    }

    @Override
    public void activate() {
        // TODO replace startSpeechDetection with this method
    }

    @Override
    public void deactivate() {
        // TODO replace finishSpeechDetection with this method
    }

    @Override
    public void start() {
        // FIXME I have to make this method public to use it in SpeechDetectorImplTest
        super.start();
    }

    @Override
    public void stop() {
        // FIXME I have to make this method public to use it in SpeechDetectorImplTest
        super.stop();
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
        return mean <= silenceLevel;
    }

    private static double mean(int[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

}
