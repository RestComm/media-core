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
 * @author amit bhayani
 *
 */
public class CleanApertureBox extends Box {

	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_c, AsciiTable.ALPHA_l, AsciiTable.ALPHA_a, AsciiTable.ALPHA_p };
	static String TYPE_S = "clap";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	private long cleanApertureWidthN;
	private long cleanApertureWidthD;

	private long cleanApertureHeightN;
	private long cleanApertureHeightD;

	private long horizOffN;
	private long horizOffD;

	private long vertOffN;
	private long vertOffD;

	public CleanApertureBox(long size) {
		super(size, TYPE_S);
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		int count = 8;

		cleanApertureWidthN = readU32(fin);
		cleanApertureWidthD = readU32(fin);

		cleanApertureHeightN = readU32(fin);
		cleanApertureHeightD = readU32(fin);

		horizOffN = readU32(fin);
		horizOffD = readU32(fin);

		vertOffN = readU32(fin);
		vertOffD = readU32(fin);

		count = 8 + 32;
		return count;
	}

	public long getCleanApertureWidthN() {
		return cleanApertureWidthN;
	}

	public long getCleanApertureWidthD() {
		return cleanApertureWidthD;
	}

	public long getCleanApertureHeightN() {
		return cleanApertureHeightN;
	}

	public long getCleanApertureHeightD() {
		return cleanApertureHeightD;
	}

	public long getHorizOffN() {
		return horizOffN;
	}

	public long getHorizOffD() {
		return horizOffD;
	}

	public long getVertOffN() {
		return vertOffN;
	}

	public long getVertOffD() {
		return vertOffD;
	}

}
