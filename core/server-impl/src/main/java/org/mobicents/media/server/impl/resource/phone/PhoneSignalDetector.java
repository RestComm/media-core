/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.resource.phone;

import java.io.IOException;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.NotifyEventImpl;
import org.mobicents.media.server.impl.resource.GoertzelFilter;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 *
 * @author kulikov
 */
public class PhoneSignalDetector extends AbstractSink {

    private final static int STATE_IDLE = 0;
    private final static int STATE_SIGNAL = 1;
    private final static int STATE_SILENCE = 2;
    private int POWER = 10000;
    private int state = STATE_IDLE;
    private final static double E = 100;
    private final static int PACKET_DURATION = 50;
    private final static Format[] FORMATS = new Format[]{Codec.LINEAR_AUDIO};
    private int[] T;
    private int[] f;
    private int offset;
    private int toneDuration = PACKET_DURATION;
    private int N = 8 * toneDuration;
    private double scale = (double) toneDuration / (double) 1000;
    private GoertzelFilter[] freqFilters;
    private double[] signal;
    private double maxAmpl;
    private double threshold;
    private int level;
    private double p[];
    private long startTime;
    private int count;
    private NotifyEvent event;

    public PhoneSignalDetector(String name) {
        super(name);
        signal = new double[N];
    }

    public int getEventID() {
        return event != null ? event.getEventID() : 0;
    }

    public void setEventID(int eventID) {
        event = new NotifyEventImpl(this, eventID);
    }

    public void setPeriods(int[] T) {
        this.T = T;
    }

    public int[] getPeriods() {
        return T;
    }

    public void setFrequency(int[] f) {
        this.f = f;
        freqFilters = new GoertzelFilter[f.length];
        p = new double[f.length];

        for (int i = 0; i < f.length; i++) {
            freqFilters[i] = new GoertzelFilter(f[i], N, scale);
        }
    }

    public int[] getFrequency() {
        return f;
    }

    public void setVolume(int level) {
        this.level = level;
        threshold = Math.pow(Math.pow(10, level), 0.1) * Short.MAX_VALUE;
    }

    public int getVolume() {
        return level;
    }

    @Override
    public void onMediaTransfer(Buffer buffer) throws IOException {
        byte[] data = buffer.getData();

        int M = data.length;
        int k = 0;
        while (k < M) {
            while (offset < N && k < M - 1) {
                double s = ((data[k++] & 0xff) | (data[k++] << 8));
                double sa = Math.abs(s);
                if (sa > maxAmpl) {
                    maxAmpl = sa;
                }
                signal[offset++] = s;
            }

            //if dtmf buffer full check signal
            if (offset == N) {
                offset = 0;
                //and if max amplitude of signal is greater theshold
                //try to detect tone.
                if (maxAmpl >= threshold) {
                    maxAmpl = 0;
                    getPower(freqFilters, signal, 0, p);
                    if (isDetected()) {
                        notifySignal();
                    } else {
                        notifyNoSignal();
                    }
                } else {
                    notifySilence();
                }
            }
        }
    }

    private void notifySignal() {
        switch (state) {
            case STATE_IDLE:
                startTime = System.currentTimeMillis();
                state = STATE_SIGNAL;
                break;
            case STATE_SILENCE:
                long now = System.currentTimeMillis();
                long duration = now - startTime;
                if (Math.abs(duration - T[1] * 1000) < E) {
                    state = STATE_SIGNAL;
                    startTime = now;
                } else {
                    count = 0;
                    state = STATE_IDLE;
                }
                break;
        }
    }

    private void notifySilence() {
        switch (state) {
            case STATE_SIGNAL:
                long now = System.currentTimeMillis();
                long duration = now - startTime;
                if (Math.abs(duration - T[0] * 1000) < E) {
                    count++;
                    if (count == 3) {
                        state = STATE_IDLE;
                        count = 0;
                        sendEvent(event);
                    } else {
                        state = STATE_SILENCE;
                        startTime = now;
                    }
                } else {
                    count = 0;
                    state = STATE_IDLE;
                }
                break;
        }
    }

    private void notifyNoSignal() {
        switch (state) {
            case STATE_SIGNAL:
                count = 0;
                state = STATE_IDLE;
                break;
        }
    }

    private boolean isDetected() {
        for (double P : p) {
            if (P <= POWER) {
                return false;
            }
        }
        return true;
    }

    private void getPower(GoertzelFilter[] filters, double[] data, int offset, double[] power) {
        for (int i = 0; i < filters.length; i++) {
            power[i] = filters[i].getPower(data, offset);
        }
    }

    public Format[] getFormats() {
        return FORMATS;
    }

    public boolean isAcceptable(Format format) {
        return format.matches(Codec.LINEAR_AUDIO);
    }
}
