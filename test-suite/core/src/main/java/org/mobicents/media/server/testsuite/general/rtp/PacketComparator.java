package org.mobicents.media.server.testsuite.general.rtp;

import java.util.Comparator;

public class PacketComparator implements Comparator<RtpPacket>
{

	public int compare(RtpPacket o1, RtpPacket o2) {
		if(o2 == null)
		return 1;
		if(o1 ==null)
			return -1;
		if(o1 == o2)
		{
			return 0;
		}
		
		return o1.getSeqNumber()-o2.getSeqNumber();
	}
	
}