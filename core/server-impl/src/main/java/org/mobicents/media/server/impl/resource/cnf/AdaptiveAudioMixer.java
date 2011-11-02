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
package org.mobicents.media.server.impl.resource.cnf;

/**
 * 
 * @author Oleg Kulikov
 * @author Vladimir Ralev
 */
public class AdaptiveAudioMixer extends AudioMixer {

    private double targetGain = 1;
    private double currentGain = 1;
    private static double maxStepDown = 1. / 22; // 51 samples transition
    // from gain 1 to gain 0
    private static double maxStepUp = 1. / 4000; // 3000 samples transition
    // from gain 1 to gain 0

    /**
     * Creates a new instance of AudioMixer.
     * 
     * @param packetPeriod
     *            packetization period in milliseconds.
     * @param fmt
     *            format of the output stream.
     */
    public AdaptiveAudioMixer(String name) {
        super(name);
    }
    
    @Override
    public byte[] mix(byte[][] input) {
        int numSamples = packetSize >> 1;
        int numChannels = input.length;
        short[][] inputs = new short[input.length][];
        for (int q = 0; q < numChannels; q++) {
            inputs[q] = byteToShortArray(input[q]);
        }

        int[] mixed = new int[numSamples];

        for (int q = 0; q < numSamples; q++) {
            for (int w = 0; w < input.length; w++) {
                mixed[q] += inputs[w][q];
            }
        }

        int numExceeding = 0;
        int maxExcess = 0;

        for (int q = 0; q < numSamples; q++) {
            int excess = 0;
            int overflow = mixed[q] - Short.MAX_VALUE;
            int underflow = mixed[q] - Short.MIN_VALUE;

            if (overflow > 0) {
                excess = overflow;
            } else if (underflow < 0) {
                excess = -underflow;
            }

            if (excess > 0) {
                numExceeding++;
            }
            maxExcess = Math.max(maxExcess, excess);
        }

        if (numExceeding > numSamples >> 5) {
            targetGain = (float) (Short.MAX_VALUE) / (float) (Short.MAX_VALUE + maxExcess + 2000);
        } else {
            targetGain = 1;
        }

        byte[] data = new byte[packetSize];
        int l = 0;
        for (int q = 0; q < numSamples; q++) {
            mixed[q] *= currentGain;
            if (targetGain - currentGain >= maxStepUp) {
                currentGain += maxStepUp;
            } else if (currentGain - targetGain > maxStepDown) {
                currentGain -= maxStepDown;
            }
            short s = (short) (mixed[q]);
            data[l++] = (byte) (s);
            data[l++] = (byte) (s >> 8);
        }
        return data;
    }
}
