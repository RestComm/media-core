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

package org.mobicents.media.control.mgcp.pkg.r;

import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.r.rto.RtpTimeout;
import org.mobicents.media.control.mgcp.pkg.r.rto.RtpTimeoutStartTime;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpEventProvider {

    public MgcpEvent provide(MgcpRequestedEvent event) {
        String packageName = event.getPackageName();

        if (RtpPackage.PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            RtpEventType eventType = RtpEventType.fromSymbol(event.getEventType());
            if (eventType != null) {
                switch (eventType) {
                    case RTP_TIMEOUT:
                        return parseRtpTimeout(event.getParameters());

                    default:
                        break;
                }
            }
        }
        return null;
    }

    private RtpTimeout parseRtpTimeout(String... parameters) {
        int timeout = 0;
        RtpTimeoutStartTime when = RtpTimeoutStartTime.IMMEDIATE;
        
        for (String parameter : parameters) {
            if(parameter.matches("\\d+")) {
                
            }
            
            int indexOfEqual = parameter.indexOf("=");
            if(indexOfEqual == -1) {
                timeout = Integer.valueOf(parameter);
            } else {
                String whenSymbol = parameter.substring(indexOfEqual + 1);
                when = RtpTimeoutStartTime.fromSymbol(whenSymbol);
            }
        }
        
        
        
        return null;
    }

}
