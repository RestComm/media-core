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

package org.restcomm.media.codec.opus;

/**
 * Implements access to JNI layer for native Opus library.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * 
 */
public class OpusJni {
    
    public final static int OPUS_APPLICATION_VOIP                   = 2048;
    public final static int OPUS_APPLICATION_AUDIO                  = 2049;
    public final static int OPUS_APPLICATION_RESTRICTED_LOWDELAY    = 2051;

    public static interface Observer {
        public void onHello();
    }
	   
    static {
        System.loadLibrary("opus-jni-linux");
    }

    public static native long createEncoderNative(int sampleRate, int channels, int application, int bitRate);
    public static native long createDecoderNative(int sampleRate, int channels);
    public static native void releaseEncoderNative(long encoderAddress);
    public static native void releaseDecoderNative(long decoderAddress);
    public static native byte[] encodeNative(long encoderAddress, short[] pcmData);
    public static native short[] decodeNative(long decoderAddress, byte[] opusData);

    public native void sayHelloNative();
    public native void setOpusObserverNative(Observer observer);
    public native void unsetOpusObserverNative();
}
