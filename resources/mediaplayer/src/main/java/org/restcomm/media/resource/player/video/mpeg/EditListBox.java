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
 * <b>8.6.6.1 Definition</b>
 * <ul>
 * <li>Box Type: �?elst’</li>
 * <li>Container: {@link EditBox} (�?edts’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or one</li>
 * </ul>
 * <p>
 * This box contains an explicit timeline map. Each entry defines part of the track time-line: by mapping part of the
 * media time-line, or by indicating �?empty’ time, or by defining a �?dwell’, where a single time-point in the media is
 * held for a period.
 * </p>
 * <p>
 * <b>NOTE</b> Edits are not restricted to fall on sample times. This means that when entering an edit, it can be
 * necessary to (a) back up to a sync point, and pre-roll from there and then (b) be careful about the duration of the
 * first sample — it might have been truncated if the edit enters it during its normal duration. If this is audio, that
 * frame might need to be decoded, and then the final slicing done. Likewise, the duration of the last sample in an edit
 * might need slicing.
 * </p>
 * <p>
 * Starting offsets for tracks (streams) are represented by an initial empty edit. For example, to play a track from its
 * start for 30 seconds, but at 10 seconds into the presentation, we have the following edit list: <br/> Entry-count = 2
 * <br/> Segment-duration = 10 seconds <br/> Media-Time = -1 <br/> Media-Rate = 1 <br/> Segment-duration = 30 seconds
 * (could be the length of the whole track) <br/> Media-Time = 0 seconds <br/> Media-Rate = 1
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class EditListBox extends FullBox {

	// File Type = elst
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_e, AsciiTable.ALPHA_l, AsciiTable.ALPHA_s, AsciiTable.ALPHA_t };
	static String TYPE_S = "elst";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}
	// entry_count is an integer that gives the number of entries in the following table
	private long entryCount;

	// segment_duration is an integer that specifies the duration of this edit segment in units of the timescale in the
	// Movie Header Box
	private long[] segmentDuration;

	// media_time is an integer containing the starting time within the media of this edit segment (in media time scale
	// units, in composition time). If this field is set to –1, it is an empty edit. The last edit in a track shall
	// never be an empty edit. Any difference between the duration in the Movie Header Box, and the track’s duration is
	// expressed as an implicit empty edit at the end.
	private long[] mediaTime;

	// media_rate specifies the relative rate at which to play the media corresponding to this edit segment. If this
	// value is 0, then the edit is specifying a �?dwell’: the media at media-time is presented for the segment-duration.
	// Otherwise this field shall contain the value 1.
	private int[] rate;
	private int[] fraction;

	public EditListBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);

		entryCount = readU32(fin);
		segmentDuration = new long[(int)entryCount];
		mediaTime = new long[(int)entryCount];
		rate = new int[(int)entryCount];
		fraction = new int[(int)entryCount];
		for (int i = 0; i < entryCount; i++) {
			if (getVersion() == 1) {
				segmentDuration[i] = read64(fin);
				mediaTime[i] = read64(fin);
			} else {
				segmentDuration[i] = fin.readInt();
				mediaTime[i] = fin.readInt();
			}
			rate[i] = (fin.readByte() << 8) | fin.readByte();
			fraction[i] = (fin.readByte() << 8) | fin.readByte();
		}
		return (int) getSize();
	}

	public long[] getSegmentDuration() {
		return segmentDuration;
	}

	public long[] getMediaTime() {
		return mediaTime;
	}

	public int[] getRate() {
		return rate;
	}

	public int[] getFraction() {
		return fraction;
	}

}
