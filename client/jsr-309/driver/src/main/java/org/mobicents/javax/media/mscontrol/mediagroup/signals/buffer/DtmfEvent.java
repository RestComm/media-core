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

package org.mobicents.javax.media.mscontrol.mediagroup.signals.buffer;

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
