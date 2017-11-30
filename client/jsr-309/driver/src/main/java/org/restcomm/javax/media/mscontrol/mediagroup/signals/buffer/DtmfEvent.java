/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.mediagroup.signals.buffer;

import jain.protocol.ip.mgcp.pkg.MgcpEvent;

/**
 *
 * @author kulikov
 */
public class DtmfEvent extends Event {
    private String s;
    
    public DtmfEvent(MgcpEvent event) {
        switch (event.intValue()) {
            case MgcpEvent.DTMF_0:
                s = "0";
                break;
            case MgcpEvent.DTMF_1:
                s = "1";
                break;
            case MgcpEvent.DTMF_2:
                s = "2";
                break;
            case MgcpEvent.DTMF_3:
                s = "3";
                break;
            case MgcpEvent.DTMF_4:
                s = "4";
                break;
            case MgcpEvent.DTMF_5:
                s = "5";
                break;
            case MgcpEvent.DTMF_6:
                s = "6";
                break;
            case MgcpEvent.DTMF_7:
                s = "7";
                break;
            case MgcpEvent.DTMF_8:
                s = "8";
                break;
            case MgcpEvent.DTMF_9:
                s = "9";
                break;
            case MgcpEvent.DTMF_A:
                s = "A";
                break;
            case MgcpEvent.DTMF_B:
                s = "B";
                break;
            case MgcpEvent.DTMF_C:
                s = "C";
                break;
            case MgcpEvent.DTMF_D:
                s = "D";
                break;
            case MgcpEvent.DTMF_HASH:
                s = "#";
                break;
            case MgcpEvent.DTMF_STAR:
                s = "*";
        }
    }
    
    @Override
    public String toString() {
        return s;
    }
}
