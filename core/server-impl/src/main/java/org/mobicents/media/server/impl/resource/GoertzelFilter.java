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
package org.mobicents.media.server.impl.resource;

/**
 *
 * @author kulikov
 */
public class GoertzelFilter {
    
    private double[] hamming;
    private double rw, iw;
    
    /** 
     * Creates a new instance of the Goertzel Filter. 
     */
    public GoertzelFilter() {
    }
    
    /** 
     * Creates a new instance of the Goertzel Filter for predefined frequency of the signal. 
     * 
     * @param the length of this signal is samples.
     * @param len the length of the signal is seconds.
     * @param the frequency of the signal.
     */
    public GoertzelFilter(int f, int N, double len) {
        hamming = new double[N];
        double o = 2* Math.PI / N;
        for (int i = 0; i < N; i++) {
            hamming[i] = (0.54-0.46 * Math.cos(o* i));
        }
        
        rw = 2.0 * Math.cos(o* len * f);
        iw = Math.sin(o * len * f);        
    }
    
    /**
     * Calculates power of the specified frequence of the specified signal.
     * 
     * @param f the frequence value.
     * @param signal sampled signal.
     * @param offset index of the first sample of the signal.
     * @param len the length of signal in samples
     * @param scale the length of signal in seconds
     * @return the power of the specified frequency
     */
    public double getPower(double f, double[] signal, int offset, int len, double scale) {
        int N = len;
        int M = N + offset;
        
        double o = 2* Math.PI / N;
        
        //hamming window
        for (int i = offset; i < M; i++) {
            signal[i] *= (0.54-0.46 * Math.cos(o* i));
        }
        
        //Goertzel filter
        double realW = 2.0 * Math.cos(o* scale* f);
        double imagW = Math.sin(o * scale* f);
        
        double d1 = 0.0;
        double d2 = 0.0;
        double y = 0;
        
        for (int n = offset; n < M; ++n) {
            y  = signal[n] + realW*d1 - d2;
            d2 = d1;
            d1 = y;
        }
        
        double resultr = 0.5*realW*d1 - d2;
        double resulti = imagW*d1;
        
        return Math.sqrt( (resultr * resultr) + (resulti * resulti));
    } 

    /**
     * Calculates power of the specified frequence of the specified signal.
     * 
     * @param signal sampled signal.
     * @param offset index of the first sample of the signal.
     * @return the power of the specified frequency
     */
    public double getPower(double[] signal, int offset) {
        int M = hamming.length + offset;
        
        //hamming window
        for (int i = offset; i < M; i++) {
            signal[i] *= hamming[i-offset];
        }
        
        //Goertzel filter
        double d1 = 0.0;
        double d2 = 0.0;
        double y = 0;
        
        for (int n = offset; n < M; ++n) {
            y  = signal[n] + rw*d1 - d2;
            d2 = d1;
            d1 = y;
        }
        
        double resultr = 0.5*rw*d1 - d2;
        double resulti = iw*d1;
        
        return Math.sqrt( (resultr * resultr) + (resulti * resulti));
    } 
    
}
