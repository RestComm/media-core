/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
 * by the @authors tag.
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

package org.restcomm.media.core.resource.dtmf;

/**
 * Out of band DTMF detector based on RFC 2833 spec.
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public class Rfc2833DtmfDetector extends AbstractDtmfDetector {

    private final static String[] evtID = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "A", "B", "C", "D" };

    private final DtmfDetectorProvider dtmfProvider = null;

    private DtmfDetector detector;

    private byte currTone = (byte) 0xFF;
    private long latestSeq = 0;

    private boolean hasEndOfEvent = false;
    private long endSeq = 0;

    private final int toneInterval;

    //The absolute time of last arrived tone measured in milliseconds
    //using wall clock
    private long lastActivity = System.currentTimeMillis();

    //last arrived tone
    private String lastSymbol;

    public Rfc2833DtmfDetector(int toneInterval) {
        this.toneInterval = toneInterval;
    }

    @Override
    public void detect(byte[] data, long duration, long sequenceNumber) {

        if (data.length != 4)
            return;

        boolean endOfEvent = false;
        endOfEvent = (data[1] & 0X80) != 0;

        // lets ignore end of event packets
        if (endOfEvent) {
            hasEndOfEvent = true;
            endSeq = sequenceNumber;
            return;
        }

        // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if (currTone == data[0]) {
            if (hasEndOfEvent) {
                if (sequenceNumber <= endSeq && sequenceNumber > (endSeq - 8)) {
                    // out of order , belongs to same event
                    // if comes after end of event then its new one
                    return;
                }
            } else if ((sequenceNumber < (latestSeq + 8)) && sequenceNumber > (latestSeq - 8)) {
                if (sequenceNumber > latestSeq)
                    latestSeq = sequenceNumber;

                return;
            }
        }

        hasEndOfEvent = false;
        endSeq = 0;

        latestSeq = sequenceNumber;
        currTone = data[0];
        String symbol = evtID[currTone];
        long now = System.currentTimeMillis();
        if (!symbol.equals(lastSymbol) || (now - lastActivity > toneInterval)) {
            lastActivity = now;
            lastSymbol = symbol;
	    notify(new DtmfEvent(symbol));
        }
        else
            lastActivity=now;

    }

}
