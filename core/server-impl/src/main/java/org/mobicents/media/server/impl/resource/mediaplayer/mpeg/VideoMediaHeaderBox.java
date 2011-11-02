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
 * <b>8.4.5.2 Video Media Header Box</b>
 * <p>
 * The video media header contains general presentation information, independent of the coding, for video media. Note
 * that the flags field has the value 1.
 * </p>
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class VideoMediaHeaderBox extends FullBox {

	// File Type = vmhd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_v, AsciiTable.ALPHA_m, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "vmhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	// graphicsmode specifies a composition mode for this video track, from the following enumerated set,
	// which may be extended by derived specifications:
	// copy = 0 copy over the existing image
	private int graphicsMode;

	// opcolor is a set of 3 colour values (red, green, blue) available for use by graphics modes
	private int[] opColors = new int[3];

	public VideoMediaHeaderBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		super.load(fin);
		graphicsMode = read16(fin);

		for (int i = 0; i < 3; i++) {
			opColors[i] = read16(fin);
		}
		return (int) getSize();
	}

	public int getGraphicsMode() {
		return graphicsMode;
	}

	public int[] getOpColors() {
		return opColors;
	}

}
