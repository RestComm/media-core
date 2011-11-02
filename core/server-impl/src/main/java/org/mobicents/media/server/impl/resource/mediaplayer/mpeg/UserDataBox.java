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
import java.util.ArrayList;
import java.util.List;

/**
 * <b>8.10.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?udta’</li>
 * <li>Container: Movie Box (�?moov’) or Track Box (�?trak’)</li>
 * <li>Mandatory: No</li>
 * <li>Quantity: Zero or one</li>
 * </ul>
 * <p>
 * This box contains objects that declare user information about the containing box and its data (presentation or
 * track).
 * </p>
 * <p>
 * The User Data Box is a container box for informative user-data. This user data is formatted as a set of boxes with
 * more specific box types, which declare more precisely their content.
 * </p>
 * <p>
 * Only a copyright notice is defined in this specification.
 * </p>
 * 
 * @author amit bhayani
 * 
 */
public class UserDataBox extends Box {

	// File Type = udta
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_u, AsciiTable.ALPHA_d, AsciiTable.ALPHA_t, AsciiTable.ALPHA_a };
	static String TYPE_S = "udta";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private List<Box> userDefinedBoxes = new ArrayList<Box>();
	private boolean isTrackParent;

	public UserDataBox(long size, boolean isTrackParent) {
		super(size, TYPE_S);
		this.isTrackParent = isTrackParent;
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;

		if (this.getSize() > 16) {
			while ((count + 8) < this.getSize()) {
				long len = readU32(fin);
				byte[] type = read(fin);
				Box box = null;
				if (comparebytes(type, CopyrightBox.TYPE)) {
					box = new CopyrightBox(len);
					count += box.load(fin);
				} else if (comparebytes(type, TrackHintInformation.TYPE) && this.isTrackParent) {
					box = new TrackHintInformation(len);
					count += box.load(fin);
				} else if (comparebytes(type, MovieHintInformation.TYPE)) {
					box = new MovieHintInformation(len);
					count += box.load(fin);
				} else {
					box = new UndefinedBox(len, new String(type));
					count += box.load(fin);
				}

				userDefinedBoxes.add(box);
			}
			
			if(count < this.getSize()){
				int skip = (int)this.getSize() - count;
				fin.skipBytes(skip);
			}
		} else {
			int skip = (int)this.getSize() - 8;
			fin.skipBytes(skip);
		}
		return (int) this.getSize();
	}

	public List<Box> getUserDefinedBoxes() {
		return userDefinedBoxes;
	}
}
