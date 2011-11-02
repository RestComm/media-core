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
package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class AudioSampleEntry extends SampleEntry {

	// ChannelCount is either 1 (mono) or 2 (stereo)
	private int channelCount;

	// SampleSize is in bits, and takes the default value of 16
	private int sampleSize = 16;

	// SampleRate is the sampling rate expressed as a 16.16 fixed-point number (hi.lo)
	private double sampleRate;

	public AudioSampleEntry(long size, String type) {
		super(size, type);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		// int[2] reserved
		fin.skip(8);

		channelCount = read16(fin);
		sampleSize = read16(fin);

		fin.skip(2);

		// reserved
		fin.skip(2);

		sampleRate = readFixedPoint1616(fin);//(a >> 16) + (a & 0xffff) / 10;

		int count = 28 + 8;

		return count;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public double getSampleRate() {
		return sampleRate;
	}

}
