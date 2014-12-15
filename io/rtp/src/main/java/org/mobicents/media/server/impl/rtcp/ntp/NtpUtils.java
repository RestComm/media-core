package org.mobicents.media.server.impl.rtcp.ntp;

/**
 * Collection of utility functions that help compute NTP timestamps for RTCP
 * reports.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class NtpUtils {

	/**
	 * Calculates the time stamp of the last received SR.
	 * 
	 * @param ntp
	 *            The most significant word of the NTP time stamp
	 * @return The middle 32 bits out of 64 in the NTP timestamp received as
	 *         part of the most recent RTCP sender report (SR).
	 */
	public static long calculateLastSrTimestamp(long ntp1, long ntp2) {
		byte[] high = uIntLongToByteWord(ntp1);
		byte[] low = uIntLongToByteWord(ntp2);
		low[3] = low[1];
		low[2] = low[0];
		low[1] = high[3];
		low[0] = high[2];
		return bytesToUIntLong(low, 0);
	}
	
	/** 
	 * Converts an unsigned 32 bit integer, stored in a long, into an array of bytes.
	 * 
	 * @param j a long
	 * @return byte[4] representing the unsigned integer, most significant bit first. 
	 */
	private static byte[] uIntLongToByteWord(long j) {
		int i = (int) j;
		byte[] byteWord = new byte[4];
		byteWord[0] = (byte) ((i >>> 24) & 0x000000FF);
		byteWord[1] = (byte) ((i >> 16) & 0x000000FF);
		byteWord[2] = (byte) ((i >> 8) & 0x000000FF);
		byteWord[3] = (byte) (i & 0x00FF);
		return byteWord;
	}
	
	/** 
	 * Combines four bytes (most significant bit first) into a 32 bit unsigned integer.
	 * 
	 * @param bytes
	 * @param index of most significant byte
	 * @return long with the 32 bit unsigned integer
	 */
	private static long bytesToUIntLong(byte[] bytes, int index) {
		long accum = 0;
		int i = 3;
		for (int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[index + i] & 0xff ) ) << shiftBy;
			i--;
		}
		return accum;
	}
	
	
}
