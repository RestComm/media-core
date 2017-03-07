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
 * <b>8.3.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?trak’</li>
 * <li>Container: {@link MovieBox} (�?moov’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: One or more</li>
 * </ul>
 * <p>
 * This is a container box for a single track of a presentation. A presentation consists of one or more tracks. Each
 * track is independent of the other tracks in the presentation and carries its own temporal and spatial information.
 * Each track will contain its associated Media Box.
 * </p>
 * <p>
 * Tracks are used for two purposes: (a) to contain media data (media tracks) and (b) to contain packetization
 * information for streaming protocols (hint tracks).
 * </p>
 * <p>
 * There shall be at least one media track within an ISO file, and all the media tracks that contributed to the hint
 * tracks shall remain in the file, even if the media data within them is not referenced by the hint tracks; after
 * deleting all hint tracks, the entire un-hinted presentation shall remain.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class TrackBox extends Box {

	// File Type = trak
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_t, AsciiTable.ALPHA_r, AsciiTable.ALPHA_a, AsciiTable.ALPHA_k };
	static String TYPE_S = "trak";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private TrackHeaderBox trackHeaderBox;
	private EditBox editBox;
	private MediaBox mediaBox;
	private TrackReferenceBox trackReferenceBox;
	private UserDataBox userDataBox;

	public TrackBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		long len = readU32(fin);

		byte[] type = read(fin);

		if (!comparebytes(type, TrackHeaderBox.TYPE)) {
			throw new IOException("Track header expected");
		}

		trackHeaderBox = new TrackHeaderBox(len);
		count += trackHeaderBox.load(fin);

		while (count < getSize()) {
			len = readU32(fin);
			type = read(fin);

			if (comparebytes(type, EditBox.TYPE)) {
				editBox = new EditBox(len);
				count += editBox.load(fin);
			} else if (comparebytes(type, MediaBox.TYPE)) {
				mediaBox = new MediaBox(len);
				count += mediaBox.load(fin);
			} else if (comparebytes(type, TrackReferenceBox.TYPE)) {
				trackReferenceBox = new TrackReferenceBox(len);
				count += trackReferenceBox.load(fin);
			} else if (comparebytes(type, UserDataBox.TYPE)) {
				userDataBox = new UserDataBox(len, true);
				count += userDataBox.load(fin);
			} else {
				throw new IOException("Unknown box=" + new String(type) + " Parent = TrackBox");
			}

		}
		return (int) this.getSize();
	}

	public TrackHeaderBox getTrackHeaderBox() {
		return trackHeaderBox;
	}

	public EditBox getEditBox() {
		return editBox;
	}

	public MediaBox getMediaBox() {
		return mediaBox;
	}

	public TrackReferenceBox getTrackReferenceBox() {
		return trackReferenceBox;
	}

	public UserDataBox getUserDataBox() {
		return userDataBox;
	}
}
