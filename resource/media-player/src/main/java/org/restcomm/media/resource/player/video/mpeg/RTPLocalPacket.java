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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RTPLocalPacket {

	private int relativeTime;
	private int Pbit;
	private int Xbit;
	private int Mbit;
	private int payloadType;

	private int extraFlag;
	private int bframeFlag;
	private int repeatFlag;

	private int rtpSequenceSeed;
	
	//This is defined by us. 
	private byte[] payload;
	private long rtpTimestamp;

	
	private int MandPayloadType;
	

	private List<RTPConstructor> rtConsList = new ArrayList<RTPConstructor>();

	public int load(RandomAccessFile raAccFile) throws IOException {
		int count = 12;
		relativeTime = raAccFile.readInt();
		int PandXbit = raAccFile.read();
		// TODO : calculate the P and X bit here		
		
		Pbit = (PandXbit & 1 << 6);
		Xbit = ((PandXbit & 1 << 5) >> 5);

		MandPayloadType = raAccFile.read();
		Mbit = (MandPayloadType & 128) >> 7;
		payloadType = (MandPayloadType & 127);

		rtpSequenceSeed = (raAccFile.read() << 8) | raAccFile.read();

		// reserved
		raAccFile.read();

		int flags = raAccFile.read();

		extraFlag = (flags & 4) >> 2;
		bframeFlag = (flags & 2) >> 1;
		repeatFlag = (flags & 1);

		int entrycount = (raAccFile.read() << 8) | raAccFile.read();

		if (extraFlag == 1) {
			int extraInfLength = ((raAccFile.read() << 24) | (raAccFile.read() << 16) | (raAccFile.read() << 8) | (raAccFile
					.read() << 0));
			count += extraInfLength;

			// TODO have RtpOffsetTLV objects created

			int skipped = 0;
			while (skipped < extraInfLength) {
				skipped += raAccFile.skipBytes(extraInfLength);
			}

		}

		for (int i = 0; i < entrycount; i++) {
			int type = raAccFile.read();
			RTPConstructor rtpConstructor = null;
			switch (type) {
			case RTPNoOpConstructor.TYPE:
				rtpConstructor = new RTPNoOpConstructor();
				count += rtpConstructor.load(raAccFile);
				break;
			case RTPImmediateConstructor.TYPE:
				rtpConstructor = new RTPImmediateConstructor();
				count += rtpConstructor.load(raAccFile);
				break;
			case RTPSampleConstructor.TYPE:
				rtpConstructor = new RTPSampleConstructor();
				count += rtpConstructor.load(raAccFile);
				break;
			case RTPSampleDescriptionConstructor.TYPE:
				rtpConstructor = new RTPSampleDescriptionConstructor();
				count += rtpConstructor.load(raAccFile);
				break;
			default:
				throw new IOException("Unknown RTPConstructor Type = " + type);

			}
			rtConsList.add(rtpConstructor);
		}
		return count;
	}

	public int getRelativeTime() {
		return relativeTime;
	}

	public int getPbit() {
		return Pbit;
	}

	public int getXbit() {
		return Xbit;
	}

	public int getMbit() {
		return Mbit;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public int getExtraFlag() {
		return extraFlag;
	}

	public int getBframeFlag() {
		return bframeFlag;
	}

	public int getRepeatFlag() {
		return repeatFlag;
	}

	public int getRtpSequenceSeed() {
		return rtpSequenceSeed;
	}

	public List<RTPConstructor> RTPConstructorList() {
		return rtConsList;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public long getRtpTimestamp() {
		return rtpTimestamp;
	}
	
	public void setRtpTimestamp(long timestamp) {
		this.rtpTimestamp = timestamp;
	}
	

	
    public byte[] toByteArray(long ssrc) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        int b = 0x80;
        b |= (Pbit << 6);
        b |= (Xbit << 5);
        
        bout.write(b);
        bout.write(MandPayloadType);
        bout.write((byte) ((rtpSequenceSeed & 0xFF00) >> 8));
        bout.write((byte) (rtpSequenceSeed & 0x00FF));

        bout.write((byte) ((this.rtpTimestamp & 0xFF000000) >> 24));
        bout.write((byte) ((this.rtpTimestamp & 0x00FF0000) >> 16));
        bout.write((byte) ((this.rtpTimestamp & 0x0000FF00) >> 8));
        bout.write((byte) ((this.rtpTimestamp & 0x000000FF)));

        bout.write((byte) ((ssrc & 0xFF000000) >> 24));
        bout.write((byte) ((ssrc & 0x00FF0000) >> 16));
        bout.write((byte) ((ssrc & 0x0000FF00) >> 8));
        bout.write((byte) ((ssrc & 0x000000FF)));

        bout.write(payload, 0, payload.length);
        return bout.toByteArray();
    }
}
