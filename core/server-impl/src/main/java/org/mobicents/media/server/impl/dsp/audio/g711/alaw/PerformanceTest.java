/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.impl.dsp.audio.g711.alaw;

import org.mobicents.media.Buffer;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 */
public class PerformanceTest {
    private byte data[] = new byte[160];
    private Decoder decoder = new Decoder();
    private Encoder encoder = new Encoder();
    private void test() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            Buffer buff = new Buffer();
            buff.setData(data);
            buff.setOffset(0);
            buff.setLength(160);
            buff.setFormat(AVProfile.PCMA);
           decoder.process(buff);
           encoder.process(buff);
        }
        
        long finish = System.currentTimeMillis();
        System.out.println(finish - start);
    }
    
    public static void main(String[] args) {
        PerformanceTest t = new PerformanceTest();
        t.test();
    }
}
