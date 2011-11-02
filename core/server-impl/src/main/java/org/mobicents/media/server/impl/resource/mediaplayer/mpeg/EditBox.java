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
