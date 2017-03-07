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
 * <b>8.7.4.1 Definition</b>
 * <ul>
 * <li>Box Type: �?stsc’</li>
 * <li>Container: {@link SampleTableBox} (�?stbl’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * Samples within the media data are grouped into chunks. Chunks can be of different sizes, and the samples within a
 * chunk can have different sizes. This table can be used to find the chunk that contains a sample, its position, and
 * the associated sample description.
 * </p>
 * <p>
 * The table is compactly coded. Each entry gives the index of the first chunk of a run of chunks with the same
 * characteristics. By subtracting one entry here from the previous one, you can compute how many chunks are in this
 * run. You can convert this to a sample count by multiplying by the appropriate samples-per-chunk.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class SampleToChunkBox extends FullBox {

	// File Type = stsc
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s, AsciiTable.ALPHA_c };
	static String TYPE_S = "stsc";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	// first_chunk is an integer that gives the index of the first chunk in this run of chunks that share the same
	// samples-per-chunk and sample-description-index; the index of the first chunk in a track has the value 1 (the
	// first_chunk field in the first record of this box has the value 1, identifying that the first sample maps to the
	// first chunk).
	private long[] firstChunk;

	// samples_per_chunk is an integer that gives the number of samples in each of these chunks
	private long[] samplesPerChunk;

	// sample_description_index is an integer that gives the index of the sample entry that describes the samples in
	// this chunk. The index ranges from 1 to the number of sample entries in the Sample Description Box
	private long[] sampleDescriptionIndex;

	public SampleToChunkBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		long entryCount = readU32(fin);

		firstChunk = new long[(int)entryCount];
		samplesPerChunk = new long[(int)entryCount];
		sampleDescriptionIndex = new long[(int)entryCount];
		for (int i = 0; i < entryCount; i++) {
			firstChunk[i] = readU32(fin);
			samplesPerChunk[i] = readU32(fin);
			sampleDescriptionIndex[i] = readU32(fin);
		}

		return (int) this.getSize();
	}

	public long[] getFirstChunk() {
		return firstChunk;
	}

	public long[] getSamplesPerChunk() {
		return samplesPerChunk;
	}

	public long[] getSampleDescriptionIndex() {
		return sampleDescriptionIndex;
	}

}
