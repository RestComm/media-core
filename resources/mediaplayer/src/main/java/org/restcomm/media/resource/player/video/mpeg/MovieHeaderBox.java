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
import java.util.Calendar;
import java.util.Date;

/**
 * <b>8.2.2.1 Definition</b>
 * <ul>
 * <li>Box Type: �?mvhd’</li>
 * <li>Container: {@link MovieBox} (�?moov’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * This box defines overall information which is media-independent, and relevant to the entire presentation considered
 * as a whole.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MovieHeaderBox extends FullBox {

	// File Type = moov
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_v, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "mvhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	/**
	 * is an integer that declares the creation time of the presentation (in seconds since midnight, Jan. 1, 1904, in
	 * UTC time
	 */
	private long creationTime;
	/**
	 * is an integer that declares the most recent time the presentation was modified (inseconds since midnight, Jan. 1,
	 * 1904, in UTC time)
	 */
	private long modificationTime;
	/**
	 * is an integer that specifies the time-scale for the entire presentation; this is the number of time units that
	 * pass in one second. For example, a time coordinate system that measures time in sixtieths of a second has a time
	 * scale of 60.
	 */
	private long timescale;
	/**
	 * is an integer that declares length of the presentation (in the indicated timescale). This property is derived
	 * from the presentation�s tracks: the value of this field corresponds to the duration of the longest track in the
	 * presentation
	 */
	private long duration;

	/** indicates the preferred rate to play the presentation */
	private double rate;
	/** indicates the preferred playback volume */
	private double volume;

	/** provides a transformation matrix for the video */
	private int[] matrix = new int[9];
	private int[] predefined = new int[6];

	/**
	 * is a non-zero integer that indicates a value to use for the track ID of the next track to be added to this
	 * presentation. Zero is not a valid track ID value. The value of next_track_ID shall be larger than the largest
	 * track-ID in use. If this value is equal to all 1s (32-bit maxint), and a new mediatrack is to be added, then a
	 * search must be made in the file for an unused track identifier.
	 */
	private int nextTrackID;
	private Calendar calendar = Calendar.getInstance();

	public MovieHeaderBox(long size) {
		super(size, TYPE_S);
	}

	/**
	 * Gets the creation time of this presentation.
	 * 
	 * @return creation time
	 */
	public Date getCreationTime() {
		calendar.set(Calendar.YEAR, 1904);
		calendar.set(Calendar.MONTH, Calendar.JANUARY);
		calendar.set(Calendar.DATE, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		calendar.roll(Calendar.SECOND, (int) creationTime);
		return calendar.getTime();
	}

	/**
	 * Gets the modification time of this presentation.
	 * 
	 * @return creation time
	 */
	public Date getModificationTime() {
		return new Date(modificationTime);
	}

	/**
	 * Gets the time scale of this presentation.
	 * 
	 * @return the number of time units that pass in one second. For example, a time coordinate system that measures
	 *         time in sixtieths of a second has a time scale of 60.
	 */
	public long getTimeScale() {
		return timescale;
	}

	/**
	 * Gets duration of this presentation.
	 * 
	 * @return an integer that declares length of the presentation (in the indicated timescale). This property is
	 *         derived from the presentation�s tracks: the value of this field corresponds to the duration of the
	 *         longest track in the presentation
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Gets the preffered rate to play the presentation.
	 * 
	 * @return the rate value, value 1.0 is normal forward playback
	 */
	public double getRate() {
		return rate;
	}

	/**
	 * Gets the preffered volume to play the presentation.
	 * 
	 * @return the rate value, value 1.0 is full volume
	 */
	public double getVolume() {
		return volume;
	}

	public int[] getMatrix() {
		return matrix;
	}

	/**
	 * Gets identifier of next track to use.
	 * 
	 * @return a non-zero integer that indicates a value to use for the track ID
	 */
	public int getNextTrackID() {
		return nextTrackID;
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		if (this.getVersion() == 1) {
			this.creationTime = readU64(fin);
			this.modificationTime = readU64(fin);
			this.timescale = readU32(fin);
			this.duration = readU64(fin);
		} else {
			this.creationTime = readU32(fin);
			this.modificationTime = readU32(fin);
			this.timescale = readU32(fin);
			this.duration = readU32(fin);
		}

		// reading rate. it is a fixed point 16.16 number that indicates the
		// preferred rate to play the presentation
		int a = fin.readInt();
		rate = (a >> 16) + (a & 0xffff) / 10;

		// reading volume. it is a fixed 8.8 number
		volume = fin.readByte() + fin.readByte() / 10;

		// skip reserved 16bits
		fin.readByte();
		fin.readByte();

		fin.readInt();
		fin.readInt();

		for (int i = 0; i < matrix.length; i++) {
			matrix[i] = fin.readInt();
		}

		for (int i = 0; i < predefined.length; i++) {
			predefined[i] = fin.readInt();
		}

		this.nextTrackID = fin.readInt();
		return (int) this.getSize();
	}

}
