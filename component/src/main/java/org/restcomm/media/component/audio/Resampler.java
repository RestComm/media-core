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

package org.restcomm.media.component.audio;

/**
 * Peforms resampling of the signal.
 *
 * @author kulikov
 */
public class Resampler {
    private int f;
    private int F;

    /**
     * Creates new resampler.
     *
     * @param f the sampling rate of the original signal in Hertz
     * @param F the sampling rate of the new signal in Hertz.
     */
    public Resampler(int f, int F) {
        this.f = f;
        this.F = F;
    }

    /**
     * Performs resampling of the given signal.
     *
     * @param buffer the buffer containing the signal.
     * @param len the length of the signal in bytes.
     * @return resampled signal
     */
    public double[] perform(double[] buffer, int len) {
        int size = (int)((double)F/f * len);
        double signal[] = new double[size];

        double dx = 1./(double)f;
        double dX = 1./(double)F;

        signal[0] = buffer[0];

        double k = 0;
        for (int i = 1; i < size - 1; i++) {
            double X = i * dX;

            int p = (int)(X/dx);
            int q = p + 1;

            k = (buffer[q] - buffer[p])/dx;
            double x = p * dx;

            signal[i] = buffer[p] + (X - x) * k;
        }

        signal[size - 1] = buffer[len - 1] + ((size - 1) *dX - (len -1)*dx) *k;
        return signal;
    }
}
