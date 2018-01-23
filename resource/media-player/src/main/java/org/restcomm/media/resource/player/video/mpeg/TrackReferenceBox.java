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
import java.util.ArrayList;
import java.util.List;

/**
 * <b>8.3.3.1 Definition</b>
 * <ul>
 * <li>Box Type: `tref’</li>
 * <li>Container: {@link TrackBox} (�?trak’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or one</li>
 * </ul>
 * <p>
 * This box provides a reference from the containing track to another track in the presentation. These references are
 * typed. A �?hint’ reference links from the containing hint track to the media data that it hints. A content description
 * reference �?cdsc’ links a descriptive or metadata track to the content which it describes.
 * </p>
 * <p>
 * Exactly one Track Reference Box can be contained within the Track Box.
 * </p>
 * <p>
 * If this box is not present, the track is not referencing any other track in any way. The reference array is sized to
 * fill the reference type box.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class TrackReferenceBox extends Box {

	// File Type = tref
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_t, AsciiTable.ALPHA_r, AsciiTable.ALPHA_e, AsciiTable.ALPHA_f };
	static String TYPE_S = "tref";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private List<TrackReferenceTypeBox> trackReferenceTypeBoxes = new ArrayList<TrackReferenceTypeBox>();

	public TrackReferenceBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		while (count < this.getSize()) {
			TrackReferenceTypeBox typeRefBox = null;
			long len = readU32(fin);
			byte[] type = read(fin);

			if (comparebytes(type, HintTrackReferenceTypeBox.TYPE)) {
				typeRefBox = new HintTrackReferenceTypeBox(len);
				count += typeRefBox.load(fin);
			} else if (comparebytes(type, CdscTrackReferenceTypeBox.TYPE)) {
				typeRefBox = new CdscTrackReferenceTypeBox(len);
				count += typeRefBox.load(fin);
			} else {
				throw new IOException("Unknown box=" + new String(type));
			}
			trackReferenceTypeBoxes.add(typeRefBox);
		}
		return (int) this.getSize();
	}

	public List<TrackReferenceTypeBox> getTrackReferenceTypeBoxes() {
		return trackReferenceTypeBoxes;
	}

}
