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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * 
 * @author kulikov
 */
public abstract class Box {

	public static HashMap<byte[], String> bytetoTypeMap = new HashMap<byte[], String>();

	private long size;
	private String type;

	public Box(long size, String type) {
		this.size = size;
		this.type = type;
	}

	public long getSize() {
		return size;
	}

	public String getType() {
		return type;
	}

	protected String readType(DataInputStream in) throws IOException {
		byte[] buff = new byte[4];
		for (int i = 0; i < buff.length; i++) {
			buff[i] = in.readByte();
		}
		return new String(buff);
	}



	protected String readText(DataInputStream in) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte b = 0;
		int count = 1;
		while ((b = in.readByte()) != 0) {
			count++;
			bout.write(b);
		}
		System.out.println(count);
		return new String(bout.toByteArray(), "UTF-8");
	}

	protected byte[] read(DataInputStream in) throws IOException {
		byte[] buff = new byte[4];
		for (int i = 0; i < buff.length; i++) {
			buff[i] = in.readByte();
		}
		return buff;
	}

	protected int read32(DataInputStream in) throws IOException {
		int output = in.readInt();
		return output;
	}

	protected long readU32(DataInputStream in) throws IOException {
		return ((long) (in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read())) & 0xFFFFFFFFL;
	}
	
    protected long readU64(DataInputStream fin) throws IOException {
    	//TODO : But wouldn't this also give -ve?
        return ((readU32(fin) << 32) | readU32(fin));
    }

	protected int read24(DataInputStream fin) throws IOException {
		int output = 0;
		output = (fin.read() << 16) | (fin.read() << 8) | fin.read();
		return output;

	}

	protected int read16(DataInputStream fin) throws IOException {
		int output = 0;
		output = (fin.read() << 8) | fin.read();
		return output;

	}

	protected double readFixedPoint1616(DataInputStream fin) throws IOException {
		double output = 0.0d;
		int a = fin.readInt();
		output = (a >> 16) + (a & 0xffff) / 10;
		return output;
	}

	protected boolean comparebytes(byte[] arg1, byte[] arg2) {
		if (arg1.length != arg2.length) {
			return false;
		}
		for (int i = 0; i < arg1.length; i++) {
			if (arg1[i] != arg2[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Loads Box from stream.
	 * 
	 * @param fin
	 *            the stream to load box from
	 * @return the number of bytes readed from stream; *
	 * @throws java.io.IOException
	 *             if some I/O error occured.
	 */
	protected abstract int load(DataInputStream fin) throws IOException;
}
