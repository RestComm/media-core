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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.exception;

import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpPackage;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractSubMgcpEventProvider implements MgcpEventProvider {

    private final MgcpPackage pkg;

    public AbstractSubMgcpEventProvider(MgcpPackage pkg) {
        this.pkg = pkg;
    }

    @Override
    public MgcpEvent provide(MgcpRequestedEvent requestedEvent)
            throws MgcpPackageNotFoundException, MgcpEventNotFoundException, MalformedMgcpEventRequestException {
        final String requestedPkg = requestedEvent.getPackageName();

        if (!this.pkg.getPackageName().equalsIgnoreCase(requestedPkg)) {
            throw new MgcpPackageNotFoundException("Event request " + requestedEvent.getQualifiedName() + " wrongly targeted package " + this.pkg.getPackageName());
        }

        final String eventType = requestedEvent.getEventType();
        final boolean supported = this.pkg.isEventSupported(eventType);

        if (!supported) {
            throw new MgcpEventNotFoundException("Package " + requestedPkg + " does not support event " + eventType);
        }

        return parse(requestedEvent);
    }

    protected abstract MgcpEvent parse(MgcpRequestedEvent requestedEvent) throws MalformedMgcpEventRequestException;

}
