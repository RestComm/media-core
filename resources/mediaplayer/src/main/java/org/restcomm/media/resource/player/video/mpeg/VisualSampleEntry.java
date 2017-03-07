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
 * @author amit bhayani
 * 
 */
public class VisualSampleEntry extends SampleEntry {

	// File Type = stsd
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_m, AsciiTable.ALPHA_p, AsciiTable.DIGIT_FOUR, AsciiTable.ALPHA_v };
	static String TYPE_S = "mp4v";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private int[] predefined = new int[3];

	// width and height are the maximum visual width and height of the stream described by this sample description, in
	// pixels
	private int width;
	private int height;

	// resolution fields give the resolution of the image in pixels-per-inch, as a fixed 16.16 number
	private double horizresolution;
	private double vertresolution;

	// frame_count indicates how many frames of compressed video are stored in each sample. The default is 1, for one
	// frame per sample; it may be more than 1 for multiple frames per sample
	private int frameCount = 1;

	// Compressorname is a name, for informative purposes. It is formatted in a fixed 32-byte field, with the first byte
	// set to the number of bytes to be displayed, followed by that number of bytes of displayable data, and then
	// padding to complete 32 bytes total (including the size byte). The field may be set to 0.
	private String compressorname;

	// depth takes one of the following values 0x0018 – images are in colour with no alpha
	private int depth;

	private PixelAspectRatioBox pixelAspectRatioBox;
	private CleanApertureBox cleanApertureBox;

	private ESDBox eSDBox;

	public VisualSampleEntry(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;
		count += super.load(fin);

		// int(16) pre_defined
		// int(16) reserved
		fin.skip(4);

		count += 4;

		predefined[0] = fin.readInt();
		predefined[1] = fin.readInt();
		predefined[2] = fin.readInt();
		count += 12;

		width = this.read16(fin);
		count += 2;

		height = this.read16(fin);
		count += 2;

		horizresolution = readFixedPoint1616(fin);
		vertresolution = readFixedPoint1616(fin);
		count += 8;

		// reserved
		fin.readInt();
		count += 4;

		frameCount = read16(fin);
		count += 2;

		int length = fin.read();

		byte[] compnamebyte = new byte[length];
		fin.read(compnamebyte, 0, length);

		compressorname = new String(compnamebyte, "UTF-8");

		byte[] zerosDiscard = new byte[31 - length];
		fin.read(zerosDiscard, 0, (31 - length));

		count += 32;

		depth = read16(fin);
		count += 2;

		read16(fin);
		count += 2;

		while (count < getSize()) {
			int len = fin.readInt();
			byte[] type = read(fin);

			if (comparebytes(type, CleanApertureBox.TYPE)) {
				cleanApertureBox = new CleanApertureBox(len);
				count += cleanApertureBox.load(fin);
			} else if (comparebytes(type, PixelAspectRatioBox.TYPE)) {
				pixelAspectRatioBox = new PixelAspectRatioBox(len);
				count += pixelAspectRatioBox.load(fin);
			} else if (comparebytes(type, ESDBox.TYPE)) {
				eSDBox = new ESDBox(len);
				count += eSDBox.load(fin);
			} else {
				// TODO : But is this error?
				//System.err.println("Unknown box=" + new String(type) + " Parent SampleTableBox");
			}
		}

		return (int) getSize();
	}

	public int[] getPredefined() {
		return predefined;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public double getHorizresolution() {
		return horizresolution;
	}

	public double getVertresolution() {
		return vertresolution;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public String getCompressorname() {
		return compressorname;
	}

	public int getDepth() {
		return depth;
	}

	public PixelAspectRatioBox getPixelAspectRatioBox() {
		return pixelAspectRatioBox;
	}

	public CleanApertureBox getCleanApertureBox() {
		return cleanApertureBox;
	}

	public ESDBox getESDBox() {
		return eSDBox;
	}

}
