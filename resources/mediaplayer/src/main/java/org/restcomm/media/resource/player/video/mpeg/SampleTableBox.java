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
 * <b>8.5.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?stbl’</li>
 * <li>Container: {@link MediaInformationBox} (�?minf’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * The sample table contains all the time and data indexing of the media samples in a track. Using the tables here, it
 * is possible to locate samples in time, determine their type (e.g. I-frame or not), and determine their size,
 * container, and offset into that container.
 * </p>
 * <p>
 * If the track that contains the Sample Table Box references no data, then the Sample Table Box does not need to
 * contain any sub-boxes (this is not a very useful media track).
 * </p>
 * If the track that the SampleTableBox is contained in does reference data, then the following sub-boxes are required:
 * {@link SampleDescriptionBox}, {@link SampleSizeBox}, {@link SampleToChunkBox}, and {@link ChunkOffsetBox}.
 * Further, the {@link SampleDescriptionBox} shall contain at least one entry. A {@link SampleDescriptionBox} is
 * required because it contains the data reference index field which indicates which Data Reference Box to use to
 * retrieve the media samples. Without the Sample Description, it is not possible to determine where the media samples
 * are stored. The Sync Sample Box is optional. If the Sync Sample Box is not present, all samples are sync samples.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class SampleTableBox extends Box {

	// File Type = stbl
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_s, AsciiTable.ALPHA_t, AsciiTable.ALPHA_b, AsciiTable.ALPHA_l };
	static String TYPE_S = "stbl";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private SampleDescriptionBox sampleDescription;
	private TimeToSampleBox timeToSampleBox;
	private CompositionOffsetBox compositionOffsetBox;
	private SampleToChunkBox sampleToChunkBox;
	private SampleSizeBox sampleSizeBox;
	private ChunkOffsetBox chunkOffsetBox;
	private SyncSampleBox syncSampleBox;

	public SampleTableBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		while (count < getSize()) {
			long len = readU32(fin);
			byte[] type = read(fin);
			if (comparebytes(type, SampleDescriptionBox.TYPE)) {
				sampleDescription = new SampleDescriptionBox(len);
				count += sampleDescription.load(fin);
			} else if (comparebytes(type, TimeToSampleBox.TYPE)) {
				timeToSampleBox = new TimeToSampleBox(len);
				count += timeToSampleBox.load(fin);
			} else if (comparebytes(type, CompositionOffsetBox.TYPE)) {
				compositionOffsetBox = new CompositionOffsetBox(len);
				count += compositionOffsetBox.load(fin);
			} else if (comparebytes(type, SampleToChunkBox.TYPE)) {
				sampleToChunkBox = new SampleToChunkBox(len);
				count += sampleToChunkBox.load(fin);
			} else if (comparebytes(type, SampleSizeBox.TYPE)) {
				sampleSizeBox = new SampleSizeBox(len);
				count += sampleSizeBox.load(fin);
			} else if (comparebytes(type, ChunkOffsetBox.TYPE)) {
				chunkOffsetBox = new ChunkOffsetBox(len);
				count += chunkOffsetBox.load(fin);
			} else if (comparebytes(type, SyncSampleBox.TYPE)) {
				syncSampleBox = new SyncSampleBox(len);
				count += syncSampleBox.load(fin);
			} else {
				throw new IOException("Unknown box=" + new String(type)+" Parent SampleTableBox");
			}
		}
		return (int) getSize();
	}

	public SampleDescriptionBox getSampleDescription() {
		return sampleDescription;
	}

	public TimeToSampleBox getTimeToSampleBox() {
		return timeToSampleBox;
	}

	public SampleToChunkBox getSampleToChunkBox() {
		return sampleToChunkBox;
	}

	public SampleSizeBox getSampleSizeBox() {
		return sampleSizeBox;
	}

	public ChunkOffsetBox getChunkOffsetBox() {
		return chunkOffsetBox;
	}

	public SyncSampleBox getSyncSampleBox() {
		return syncSampleBox;
	}

	public CompositionOffsetBox getCompositionOffsetBox() {
		return compositionOffsetBox;
	}

}
