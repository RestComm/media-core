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
import java.util.Date;

/**
 * 
 * <b>8.4.2.1 Definition</b>
 * <ul>
 * <li>Box Type: �?mdhd’</li>
 * <li>Container: Media Box (�?mdia’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * The media header declares overall information that is media-independent, and relevant to characteristics of the media
 * in a track.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MediaHeaderBox extends FullBox {

	// File Type = mdhd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_d, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "mdhd";
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

	private int language;

	public MediaHeaderBox(long size) {
		super(size, TYPE_S);
	}

	/**
	 * Gets the creation time of the media in this track.
	 * 
	 * @return creation time
	 */
	public Date getCreationTime() {
		return new Date(creationTime);
	}

	/**
	 * Gets the modification time of the media in this track.
	 * 
	 * @return creation time
	 */
	public Date getModificationTime() {
		return new Date(modificationTime);
	}

	/**
	 * Gets duration of the media in this track.
	 * 
	 * @return an integer that declares length of the media in this track (in the indicated timescale).
	 */
	public long getDuration() {
		return duration;
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		if (this.getVersion() == 1) {
			this.creationTime = readU64(fin);
			this.modificationTime = readU64(fin);
			this.timescale = readU64(fin);
			this.duration = read64(fin);
		} else {
			this.creationTime = readU32(fin);
			this.modificationTime = readU32(fin);
			this.timescale = readU32(fin);
			this.duration = readU32(fin);
		}

		language = read16(fin);
		fin.readByte();
		fin.readByte();

		return (int) getSize();
	}

	/**
	 * Gets the time scale of the media in this track.
	 * 
	 * @return the number of time units that pass in one second. For example, a time coordinate system that measures
	 *         time in sixtieths of a second has a time scale of 60.
	 */
	public long getTimescale() {
		return timescale;
	}

	public int getLanguage() {
		return language;
	}

}
