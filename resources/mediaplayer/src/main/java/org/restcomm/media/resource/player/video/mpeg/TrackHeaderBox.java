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
 * <b>8.3.2.1 Definition</b>
 * <ul>
 * <li> Box Type: �?tkhd’</li>
 * <li> Container: {@link TrackBox} (�?trak’)</li>
 * <li> Mandatory: Yes</li>
 * <li> Quantity: Exactly one</li>
 * </ul>
 * <p>
 * This box specifies the characteristics of a single track. Exactly one Track Header Box is contained in a track. In
 * the absence of an edit list, the presentation of a track starts at the beginning of the overall presentation. An
 * empty edit is used to offset the start time of a track.
 * </p>
 * <p>
 * The default value of the track header flags for media tracks is 7 (track_enabled, track_in_movie, track_in_preview).
 * If in a presentation all tracks have neither track_in_movie nor track_in_preview set, then all tracks shall be
 * treated as if both flags were set on all tracks. Hint tracks should have the track header flags set to 0, so that
 * they are ignored for local playback and preview.
 * </p>
 * <p>
 * The width and height in the track header are measured on a notional �?square’ (uniform) grid. Track video data is
 * normalized to these dimensions (logically) before any transformation or placement caused by a layup or composition
 * system. Track (and movie) matrices, if used, also operate in this uniformly-scaled space.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class TrackHeaderBox extends FullBox {

	// File Type = tkhd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_t, AsciiTable.ALPHA_k, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "tkhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	// creation_time is an integer that declares the creation time of this track (in seconds since midnight, Jan. 1,
	// 1904, in UTC time)
	private long creationTime;

	// modification_time is an integer that declares the most recent time the track was modified (in seconds since
	// midnight, Jan. 1, 1904, in UTC time)
	private long modificationTime;

	// duration is an integer that indicates the duration of this track (in the timescale indicated in the Movie Header
	// Box). The value of this field is equal to the sum of the durations of all of the track’s edits. If there is no
	// edit list, then the duration is the sum of the sample durations, converted into the timescale in the Movie Header
	// Box. If the duration of this track cannot be determined then duration is set to all 1s (32-bit maxint).
	private long duration;

	// track_ID is an integer that uniquely identifies this track over the entire life-time of this presentation. Track
	// IDs are never re-used and cannot be zero.
	private long trackID;

	// layer specifies the front-to-back ordering of video tracks; tracks with lower numbers are closer to the viewer. 0
	// is the normal value, and -1 would be in front of track 0, and so on.
	private int layer;

	// alternate_group is an integer that specifies a group or collection of tracks. If this field is 0 there is no
	// information on possible relations to other tracks. If this field is not 0, it should be the same for tracks that
	// contain alternate data for one another and different for tracks belonging to different such groups. Only one
	// track within an alternate group should be played or streamed at any one time, and must be distinguishable from
	// other tracks in the group via attributes such as bitrate, codec, language, packet size etc. A group may have only
	// one member.
	private int alternateGroup;

	// volume is a fixed 8.8 value specifying the track's relative audio volume. Full volume is 1.0 (0x0100) and is the
	// normal value. Its value is irrelevant for a purely visual track. Tracks may be composed by combining them
	// according to their volume, and then using the overall Movie Header Box volume setting; or more complex audio
	// composition (e.g. MPEG-4 BIFS) may be used.
	private float volume;

	// matrix provides a transformation matrix for the video; (u,v,w) are restricted here to (0,0,1), hex
	// (0,0,0x40000000)
	private int[] matrix = new int[9];

	// width and height specify the track's visual presentation size as fixed-point 16.16 values. These need not be the
	// same as the pixel dimensions of the images, which is documented in the sample description(s); all images in the
	// sequence are scaled to this size, before any overall transformation of the track represented by the matrix. The
	// pixel dimensions of the images are the default values.
	private double width;
	private double height;

	public TrackHeaderBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		if (this.getVersion() == 1) {
			this.creationTime = read64(fin);
			this.modificationTime = read64(fin);
			this.trackID = fin.readInt();
			fin.readInt(); // spare
			this.duration = read64(fin);
		} else {
			this.creationTime = fin.readInt();
			this.modificationTime = fin.readInt();
			this.trackID = fin.readInt();
			fin.readInt(); // spare
			this.duration = fin.readInt();
		}

		// reserved
		fin.readInt();
		fin.readInt();

		// reading layer.
		layer = (fin.readByte() << 8) | fin.readByte();
		alternateGroup = (fin.readByte() << 8) | fin.readByte();

		// reading volume. it is a fixed 8.8 number
		volume = fin.readByte() + fin.readByte() / 10;

		// skip reserved 16bits
		fin.readByte();
		fin.readByte();

		for (int i = 0; i < matrix.length; i++) {
			matrix[i] = fin.readInt();
		}

		width = readFixedPoint1616(fin); 
		height = readFixedPoint1616(fin);

		return (int) getSize();
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getModificationTime() {
		return modificationTime;
	}

	public long getDuration() {
		return duration;
	}

	public long getTrackID() {
		return trackID;
	}

	public int getLayer() {
		return layer;
	}

	public int getAlternateGroup() {
		return alternateGroup;
	}

	public float getVolume() {
		return volume;
	}

	public int[] getMatrix() {
		return matrix;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

}
