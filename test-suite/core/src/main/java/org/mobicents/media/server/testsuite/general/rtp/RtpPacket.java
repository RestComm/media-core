/*
 * RtpPacket.java
 *
 * Mobicents Media Gateway
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;

import org.mobicents.media.server.testsuite.general.AbstractTestCase;

/**
 * 
 * @author Oleg Kulikov
 */
public class RtpPacket implements Serializable {

	private int version = 2;
	private boolean padding = false;
	private boolean extensions = false;
	private int cc = 0;
	private boolean marker = false;
	private int payloadType;
	private int seqNumber;
	private int timestamp;
	private long ssrc;
	private byte[] payload;
	private int offset = 0;
	private int length = 0;

	private Date time;
	private long callSequence;
	private String connectionIdentifier;
	public RtpPacket(ByteBuffer readerBuffer) {
		int len = readerBuffer.limit();
		int b = readerBuffer.get() & 0xff;

		version = (b & 0x0C) >> 6;
		padding = (b & 0x20) == 0x020;
		extensions = (b & 0x10) == 0x10;
		cc = b & 0x0F;

		b = readerBuffer.get() & 0xff;
		marker = (b & 0x80) == 0x80;
		payloadType = b & 0x7F;

		seqNumber = (readerBuffer.get() & 0xff) << 8;
		seqNumber = seqNumber | (readerBuffer.get() & 0xff);

		timestamp = readerBuffer.getInt();
		ssrc = readerBuffer.getInt();

		payload = new byte[len - 12];
		readerBuffer.get(payload, 0, payload.length);
	}

	/** Creates a new instance of RtpPacket */
	public RtpPacket(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		int b = in.read() & 0xff;
		version = (b & 0x0C) >> 6;
		padding = (b & 0x20) == 0x020;
		extensions = (b & 0x10) == 0x10;
		cc = b & 0x0F;

		b = in.read() & 0xff;

		marker = (b & 0x80) == 0x80;
		payloadType = b & 0x7F;
		seqNumber = (in.read() & 0xff) << 8;
		seqNumber = seqNumber | (in.read() & 0xff);

		timestamp = in.readInt();
		ssrc = in.readInt();

		payload = new byte[160];
		int numBytes = in.read(payload);
		if (numBytes < 0) {
			numBytes = 0;
		}
		byte[] realPayload = new byte[numBytes];
		for (int q = 0; q < numBytes; q++) {
			realPayload[q] = payload[q];
		}
		payload = realPayload;
	}

	public RtpPacket(byte payloadType, int seqNumber, int timestamp, long ssrc, byte[] payload) {
		this.payloadType = payloadType;
		this.payload = payload;
		this.seqNumber = seqNumber;
		this.timestamp = timestamp;
		this.ssrc = ssrc;
	}

	public RtpPacket(byte payloadType, int seqNumber, int timestamp, long ssrc, byte[] payload, int offset, int length) {
		this.payloadType = payloadType;
		this.payload = payload;
		this.seqNumber = seqNumber;
		this.timestamp = timestamp;
		this.ssrc = ssrc;
		this.offset = offset;
		this.length = length;
	}

	public RtpPacket() {

	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public long getSSRC() {
		return ssrc;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public int getSeqNumber() {
		return this.seqNumber;
	}

	public byte[] getPayload() {
		return payload;
	}

	public int getTimestamp() {
		return timestamp;
	}
	/**
	 * @param sequence
	 */
	public void setCallSequence(long sequence) {
		this.callSequence = sequence;
		
	}
	public long getCallSequence() {
		return this.callSequence;
		
	}
	
	public String getConnectionIdentifier() {
		return connectionIdentifier;
	}

	public void setConnectionIdentifier(String connectionIdentifier) {
		this.connectionIdentifier = connectionIdentifier;
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		bout.write(0x80);
		bout.write(payloadType);
		bout.write((byte) ((seqNumber & 0xFF00) >> 8));
		bout.write((byte) (seqNumber & 0x00FF));

		bout.write((byte) ((timestamp & 0xFF000000) >> 24));
		bout.write((byte) ((timestamp & 0x00FF0000) >> 16));
		bout.write((byte) ((timestamp & 0x0000FF00) >> 8));
		bout.write((byte) ((timestamp & 0x000000FF)));

		bout.write((byte) ((ssrc & 0xFF000000) >> 24));
		bout.write((byte) ((ssrc & 0x00FF0000) >> 16));
		bout.write((byte) ((ssrc & 0x0000FF00) >> 8));
		bout.write((byte) ((ssrc & 0x000000FF)));

		// try {
		bout.write(payload, offset, length);
		// } catch (IOException e) {
		// }

		return bout.toByteArray();
	}

	@Override
	public String toString() {
		return "RTP Packet[seq=" + this.seqNumber + ", timestamp=" + timestamp + ", payload_size=" + payload.length + "]";
	}

	/**
	 * 
	 */
	public void minimize() {
		this.payload = null;
	}

	public String serializeToString() {
		StringBuffer sb = new StringBuffer();
		//private callSequence;
		sb.append(this.callSequence);
		//private connectionIdentifier
		sb.append(",").append(this.connectionIdentifier);
		// private Date time;
		sb.append(",").append(this.time.getTime());
		// private int seqNumber;
		sb.append(",").append(seqNumber);
		// private int payloadType;
		sb.append(",").append(payloadType);
		// private int timestamp;
		sb.append(",").append(timestamp);
		// private int offset = 0;
		sb.append(",").append(offset);
		// private long ssrc;
		sb.append(",").append(ssrc);
		// private int length = 0;
		sb.append(",").append(length);
		// private int version = 2;
		sb.append(",").append(version);
		// private boolean padding = false;
		sb.append(",").append(padding);
		// private boolean extensions = false;
		sb.append(",").append(extensions);
		// private int cc = 0;
		sb.append(",").append(cc);
		// private boolean marker = false;
		sb.append(",").append(marker);
		sb.append(AbstractTestCase._LINE_SEPARATOR);
		return sb.toString();
	}

	public void deserializeFromString(String str) {
		try {
			String[] split = str.split(",");
			//private callSequence;
			this.callSequence = Long.parseLong(split[0]);
			//private connectionIdentifier
			connectionIdentifier = split[1];
			// private Date time;
			this.time = new Date(Long.parseLong(split[2]));
			// private int seqNumber;
			this.seqNumber = Integer.parseInt(split[3]);
			// private int payloadType;
			this.payloadType = Integer.parseInt(split[4]);
			// private int timestamp;
			this.timestamp = Integer.parseInt(split[5]);
			// private int offset = 0;
			this.offset = Integer.parseInt(split[6]);
			// private long ssrc;
			this.ssrc = Long.parseLong(split[7]);
			// private int length = 0;
			this.length = Integer.parseInt(split[8]);
			// private int version = 2;
			this.version = Integer.parseInt(split[9]);
			// private boolean padding = false;
			this.padding = Boolean.parseBoolean(split[10]);
			// private boolean extensions = false;
			this.extensions = Boolean.parseBoolean(split[11]);
			// private int cc = 0;
			this.length = Integer.parseInt(split[12]);
			// private boolean marker = false;
			this.marker = Boolean.parseBoolean(split[13]);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("DATA: \""+str+"\"");
		}

	}


}
