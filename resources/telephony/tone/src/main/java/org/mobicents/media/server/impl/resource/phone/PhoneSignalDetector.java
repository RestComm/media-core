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

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.MediaOutput;
import org.mobicents.media.server.component.audio.GoertzelFilter;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.listener.Event;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.tone.ToneDetector;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;

/**
 *
 * @author Oifa Yulian
 */
public class PhoneSignalDetector extends AbstractSink implements ToneDetector {

    private static final long serialVersionUID = -3631042626327972595L;

    private double POWER = 100000;
    private final static int PACKET_DURATION = 50;
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

    private MediaOutput output;

    private Listeners<ToneDetectorListener> listeners = new Listeners<ToneDetectorListener>();

    public PhoneSignalDetector(String name, Scheduler scheduler) {
        super(name);
        signal = new double[N];

        output = new MediaOutput(ComponentType.SIGNAL_DETECTOR.getType(), scheduler);
        output.join(this);
    }

    public MediaOutput getMediaOutput() {
        return this.output;
    }

    @Override
    public void setFrequency(int[] f) {
        this.f = f;
        freqFilters = new GoertzelFilter[f.length];
        p = new double[f.length];

        for (int i = 0; i < f.length; i++) {
            freqFilters[i] = new GoertzelFilter(f[i], N, scale);
        }
    }

    @Override
    public int[] getFrequency() {
        return f;
    }

    @Override
    public void setVolume(int level) {
        this.level = level;
        threshold = Math.pow(Math.pow(10, level), 0.1) * Short.MAX_VALUE;
    }

    @Override
    public int getVolume() {
        return level;
    }

    @Override
    public void activate() {
        output.start();
    }

    @Override
    public void deactivate() {
        output.stop();
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        byte[] data = frame.getData();

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

            // if dtmf buffer full check signal
            if (offset == N) {
                offset = 0;
                // and if max amplitude of signal is greater theshold
                // try to detect tone.
                if (maxAmpl >= threshold) {
                    maxAmpl = 0;
                    getPower(freqFilters, signal, 0, p);
                    int detectedValue = isDetected();
                    if (detectedValue >= 0)
                        sendEvent(new ToneEventImpl(this, getFrequency()[detectedValue]));
                }
            }
        }
    }

    private int isDetected() {
        for (int i = 0; i < p.length; i++) {
            if (p[i] >= POWER) {
                return i;
            }
        }
        return -1;
    }

    private void getPower(GoertzelFilter[] filters, double[] data, int offset, double[] power) {
        for (int i = 0; i < filters.length; i++) {
            power[i] = filters[i].getPower(data, offset);
        }
    }

    @Override
    public void addListener(ToneDetectorListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ToneDetectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearAllListeners() {
        listeners.clear();
    }

    private void sendEvent(Event<?> event) {
        listeners.dispatch(event);
    }
}
