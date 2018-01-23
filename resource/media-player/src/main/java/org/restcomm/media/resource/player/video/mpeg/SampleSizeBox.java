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

package org.restcomm.media.resource.player.video.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * <b>8.7.3.1 Definition</b>
 * <ul>
 * <li>Box Type: �?stsz’, �?stz2’</li>
 * <li>Container: Sample Table Box (�?stbl’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one variant must be present</li>
 * </ul>
 * <p>
 * This box contains the sample count and a table giving the size in bytes of each sample. This allows the media data
 * itself to be unframed. The total number of samples in the media is always indicated in the sample count.
 * </p>
 * <p>
 * There are two variants of the sample size box. The first variant has a fixed size 32-bit field for representing the
 * sample sizes; it permits defining a constant size for all samples in a track. The second variant permits smaller size
 * fields, to save space when the sizes are varying but small. One of these boxes must be present; the first version is
 * preferred for maximum compatibility
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class SampleSizeBox extends FullBox {

	// File Type = stsz
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s, AsciiTable.ALPHA_z };
	static String TYPE_S = "stsz";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	// sample_size is integer specifying the default sample size. If all the samples are the same size, this field
	// contains that size value. If this field is set to 0, then the samples have different sizes, and those sizes are
	// stored in the sample size table. If this field is not 0, it specifies the constant sample size, and no array
	// follows.
	private long sampleSize;

	// is an integer that gives the number of samples in the track; if sample-size is 0, then it is also the number of
	// entries in the following table.
	private long sampleCount;

	// entry_size is an integer specifying the size of a sample, indexed by its number.
	private long[] entrySize;

	public SampleSizeBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		sampleSize = readU32(fin);
		sampleCount = readU32(fin);

		if (sampleSize == 0) {
			entrySize = new long[(int)sampleCount];

			for (int i = 0; i < sampleCount; i++) {
				entrySize[i] = readU32(fin);
			}
		}
		return (int) this.getSize();
	}

	public long getSampleSize() {
		return sampleSize;
	}

	public long getSampleCount() {
		return sampleCount;
	}

	public long[] getEntrySize() {
		return entrySize;
	}

}
