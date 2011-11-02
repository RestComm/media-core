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
 * <b>8.7.2.1 Definition</b>
 * <ul>
 * <li>Box Types: �?url �?, �?urn �?, �?dref’</li>
 * <li>Container: Data Information Box (�?dinf’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * The data reference object contains a table of data references (normally URLs) that declare the location(s) of the
 * media data used within the presentation. The data reference index in the sample description ties entries in this
 * table to the samples in the track. A track may be split over several sources in this way.
 * </p>
 * <p>
 * If the flag is set indicating that the data is in the same file as this box, then no string (not even an empty one)
 * shall be supplied in the entry field.
 * </p>
 * <p>
 * The DataEntryBox within the DataReferenceBox shall be either a DataEntryUrnBox or a DataEntryUrlBox.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class DataReferenceBox extends FullBox {

	// File Type = stbl
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_d, AsciiTable.ALPHA_r, AsciiTable.ALPHA_e, AsciiTable.ALPHA_f };
	static String TYPE_S = "dref";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private long entryCount;
	private Box[] dataEntry;

	public DataReferenceBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		entryCount = readU32(fin);
		dataEntry = new Box[(int) entryCount];
		for (int i = 0; i < entryCount; i++) {
			long len = readU32(fin);
			String type = readType(fin);

			if (type.equals("url ")) {
				dataEntry[i] = new DataEntryUrlBox(len, type);
				dataEntry[i].load(fin);
			} else if (type.equals("urn")) {
				dataEntry[i] = new DataEntryUrnBox(len, type);
				dataEntry[i].load(fin);
			} else {
				dataEntry[i] = new UndefinedBox(len, type);
				dataEntry[i].load(fin);
			}
		}
		return (int) getSize();
	}

}
