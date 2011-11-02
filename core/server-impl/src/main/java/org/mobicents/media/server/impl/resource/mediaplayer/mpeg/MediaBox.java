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
 * <b>8.4.1.1 Definition</b>
 * <ul>
 * <li>Box Type: �?mdia’</li>
 * <li>Container: {@link TrackBox} (�?trak’)</li>
 * <li>Mandatory: Yes</li>
 * <li>Quantity: Exactly one</li>
 * </ul>
 * <p>
 * The media declaration container contains all the objects that declare information about the media data within a
 * track.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MediaBox extends Box {

	// File Type = mdia
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_d, AsciiTable.ALPHA_i, AsciiTable.ALPHA_a };
	static String TYPE_S = "mdia";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private MediaHeaderBox mediaHeaderBox;
	private HandlerReferenceBox handlerReferenceBox;
	private MediaInformationBox mediaInformationBox;

	public MediaBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;

		long len = readU32(fin);
		byte[] type = read(fin);

		if (!comparebytes(type, MediaHeaderBox.TYPE)) {
			throw new IOException("Media header box is expected");
		}

		mediaHeaderBox = new MediaHeaderBox(len);
		count += mediaHeaderBox.load(fin);

		len = readU32(fin);
		type = read(fin);

		if (!comparebytes(type, HandlerReferenceBox.TYPE)) {
			throw new IOException("Handler box is mandatory");
		}

		handlerReferenceBox = new HandlerReferenceBox(len);
		count += handlerReferenceBox.load(fin);

		len = readU32(fin);
		type = read(fin);

		if (!comparebytes(type, MediaInformationBox.TYPE)) {
			throw new IOException("MediaInformationBox box is mandatory but received "+ new String(type));
		}

		mediaInformationBox = new MediaInformationBox(len);
		count += mediaInformationBox.load(fin);

		return (int) getSize();
	}

	public MediaHeaderBox getMediaHeaderBox() {
		return mediaHeaderBox;
	}

	public HandlerReferenceBox getHandlerReferenceBox() {
		return handlerReferenceBox;
	}

	public MediaInformationBox getMediaInformationBox() {
		return mediaInformationBox;
	}

}
