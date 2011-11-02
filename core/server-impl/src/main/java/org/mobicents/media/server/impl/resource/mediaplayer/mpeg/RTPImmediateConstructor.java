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

import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * 
 * @author amit bhayani
 *
 */
public class RTPImmediateConstructor extends RTPConstructor {

	public static final int TYPE = 1;

	private int count;
	private byte[] data;

	public RTPImmediateConstructor() {
		super(TYPE);
	}

	@Override
	public int load(RandomAccessFile raAccFile) throws IOException {
		// 1 is for Type + 1 is for count
		int bytesRead = 2;

		count = raAccFile.read();
		data = new byte[count];
		for (int i = 0; i < count; i++) {
			data[i] = raAccFile.readByte();
		}		
 
		bytesRead += count;

		if (bytesRead < 16) {
			// Each Constructor needs to be 16bytes.
			raAccFile.skipBytes(16 - bytesRead);

		}
		return 16;
	}

	public int getCount() {
		return count;
	}

	public byte[] getData() {
		return data;
	}

}
