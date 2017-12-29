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

package org.restcomm.media.resource.speechdetector;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Component that detects user speech from a stream of incoming audio.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 *
 */
public class NoiseThresholdDetector implements SpeechDetector {

    Deque<Double> logEnergyThresholdHistory = new ArrayDeque<Double>();
    final int logEnergyThresholdHistoryLength = 10;

    public NoiseThresholdDetector(final int silenceLevel) {
        logEnergyThresholdHistory.addLast((double)silenceLevel);
    }

    /**
     * Checks does the sample buffer contains sound or silence.
     *
     * @param data buffer with samples
     * @param offset the position of first sample in buffer
     * @param len the number of samples
     * @return true if silence detected
     */
    @Override
    public boolean detect(byte[] data, int offset, int len) {
        short[] shortData = new short[len];
        for (int i = offset; i < len - 1; i += 2)
            shortData[i/2] = (short)(data[i] | (data[i + 1] << 8));

        final double logEnergy = logEnergy(shortData);
        final double zeroCrossing = zeroCrossing(shortData);
        final double logEnergyThreshold = getMeanLogEnergyThreshold();
        final double zeroCrossingThreshold = 0.3;
        final double logEnergyPonder = 0.9;
        final double zeroCrossingPonder = 0.1;

        boolean speechDetected = (logEnergyPonder * (logEnergy - logEnergyThreshold) -
                10 * zeroCrossingPonder * (zeroCrossing - zeroCrossingThreshold)) >= 0;

        if (speechDetected)
            addLogEnergyThreshold(0.2 * logEnergy);

        return speechDetected;
    }

    private double logEnergy(short[] data) {
        double sum = 0;
        for (int i = 0; i < data.length; i++)
            sum += Math.pow(data[i], 2);

        return Math.log10(sum / data.length);
    }

    private double zeroCrossing(short[] data) {
        double sum = 0;
        for (int i = 1; i < data.length; i++)
            sum += Math.abs(Math.signum(data[i]) - Math.signum(data[i-1]));

        return sum / (2 * data.length);
    }

    private void addLogEnergyThreshold(double logEnergyThreshold) {
        logEnergyThresholdHistory.addLast(logEnergyThreshold);
        if (logEnergyThresholdHistory.size() > logEnergyThresholdHistoryLength)
            logEnergyThresholdHistory.removeFirst();
    }

    private double getMeanLogEnergyThreshold() {
        double sum = 0;
        Iterator<Double> iter = logEnergyThresholdHistory.iterator();
        while (iter.hasNext())
            sum += iter.next();
        return sum / logEnergyThresholdHistory.size();
    }
}
