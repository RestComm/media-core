/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.pkg.au.pc;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum DtmfToneEvent {

    DTMF_0('0'),
    DTMF_1('1'),
    DTMF_2('2'),
    DTMF_3('3'),
    DTMF_4('4'),
    DTMF_5('5'),
    DTMF_6('6'),
    DTMF_7('7'),
    DTMF_8('8'),
    DTMF_9('9'),
    DTMF_A('A'),
    DTMF_B('B'),
    DTMF_C('C'),
    DTMF_D('D'),
    DTMF_HASH('#'),
    DTMF_STAR('*');

    private final char tone;

    private DtmfToneEvent(char tone) {
        this.tone = tone;
    }

    public char tone() {
        return tone;
    }

    public static final DtmfToneEvent fromTone(char tone) {
        for (DtmfToneEvent event : values()) {
            if (event.tone == tone) {
                return event;
            }
        }
        return null;
    }

}
