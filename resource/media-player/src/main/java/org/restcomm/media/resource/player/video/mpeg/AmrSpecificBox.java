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
 * The AMRSpecificBox fields for AMR and AMR-WB shall be as defined in table 6.6. The AMRSpecificBox for the
 * AMRSampleEntry Box shall always be included if the 3GP file contains AMR or AMR-WB media.
 * 
 * <p>
 * Table 6.6: The AMRSpecificBox fields for AMRSampleEntry
 * <p>
 * Look at 6.7 section of 3GPP TS 26.244 Transparent end-to-end packet switched streaming service (PSS); 3GPP file
 * format (3GP)
 * <p>
 * http://www.3gpp.org/ftp/Specs/html-info/26244.htm
 * 
 * @author amit bhayani
 * 
 */
public class AmrSpecificBox extends Box {

	private String vendor;
	private int decoderVersion;
	private int modeSet;
	private int modeChangePeriod;
	private int framesPerSample;

	public AmrSpecificBox(long size, String type) {
		super(size, type);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int load(DataInputStream fin) throws IOException {
		//Here size should 8+9 = 17
		
		int count = 8;
		
		vendor = new String(read(fin));
		decoderVersion = fin.readByte();
		modeSet = super.read16(fin);
		modeChangePeriod = fin.readByte();
		framesPerSample = fin.readByte();

		return count + 9;
	}

	public String getVendor() {
		return vendor;
	}

	public int getDecoderVersion() {
		return decoderVersion;
	}

	public int getModeSet() {
		return modeSet;
	}

	public int getModeChangePeriod() {
		return modeChangePeriod;
	}

	public int getFramesPerSample() {
		return framesPerSample;
	}

}
