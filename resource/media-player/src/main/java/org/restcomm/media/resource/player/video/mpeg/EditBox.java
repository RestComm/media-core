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
 * <b>8.6.5.1 Definition<b>
 * <ul>
 * <li>Box Type: �?edts’</li>
 * <li>Container: {@link TrackBox} (�?trak’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or one</li>
 * </ul>
 * <p>
 * An Edit Box maps the presentation time-line to the media time-line as it is stored in the file. The Edit Box is a
 * container for the edit lists.
 * </p>
 * <p>
 * The Edit Box is optional. In the absence of this box, there is an implicit one-to-one mapping of these time-lines,
 * and the presentation of a track starts at the beginning of the presentation. An empty edit is used to offset the
 * start time of a track.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class EditBox extends Box {

	// File Type = edts
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_e, AsciiTable.ALPHA_d, AsciiTable.ALPHA_t, AsciiTable.ALPHA_s };
	static String TYPE_S = "edts";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private EditListBox editList;

	public EditBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		long len = readU32(fin);
		byte[] type = read(fin);

		if (comparebytes(type, EditListBox.TYPE)) {
			editList = new EditListBox(len);
			editList.load(fin);
		}
		return (int) getSize();
	}

	public EditListBox getEditList() {
		return editList;
	}

}
