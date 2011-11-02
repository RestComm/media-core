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
package org.mobicents.media.server.impl.resource.fft;

import java.io.IOException;
import org.mobicents.media.Format;
import org.mobicents.media.Buffer;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.AbstractSink;

/**
 * 
 * @author Oleg Kulikov
 */
public class SpectraAnalyzer extends AbstractSink {

    public SpectraAnalyzer(String name) {
        super(name);
    }
    
    private final static AudioFormat LINEAR_AUDIO = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1,
            AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
    private int offset = 0;
    private byte[] localBuffer = new byte[16000];
    private FFT fft = new FFT();

    private double[] mod(Complex[] x) {
        double[] res = new double[x.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = Math.sqrt(x[i].re() * x[i].re() + x[i].im() * x[i].im());
        }
        return res;
    }

    protected void sendEvent(double[] spectra) {
        SpectrumEvent evt = new SpectrumEvent(this, spectra);
        sendEvent(evt);
    }

    public void onMediaTransfer(Buffer buffer) throws IOException {
            byte[] data = buffer.getData();

            int len = Math.min(16000 - offset, data.length);
            System.arraycopy(data, 0, localBuffer, offset, len);
            offset += len;

            // buffer full?
            if (offset == 16000) {
                double[] media = new double[8000];
                int j = 0;
                for (int i = 0; i < media.length; i++) {
                    media[i] = (localBuffer[j++] & 0xff) | (localBuffer[j++] << 8);
                }
                // resampling
                Complex[] signal = new Complex[8192];
                double k = (double) (media.length - 1) / (double) (signal.length);

                for (int i = 0; i < signal.length; i++) {
                    int p = (int) (k * i);
                    int q = (int) (k * i) + 1;

                    double K = (media[q] - media[p]) * media.length;
                    double dx = (double) i / (double) signal.length - (double) p / (double) media.length;
                    signal[i] = new Complex(media[p] + K * dx, 0);
                }
                localBuffer = new byte[16000];
                offset = 0;

                Complex[] sp = fft.fft(signal);
                double[] res = mod(sp);
                sendEvent(res);
            }
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink.isAcceptable(Format).
     */
    public boolean isAcceptable(Format fmt) {
        return fmt.matches(LINEAR_AUDIO);
    }

    public Format[] getFormats() {
        return new Format[]{LINEAR_AUDIO};
    }
}

