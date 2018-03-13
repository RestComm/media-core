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

package org.restcomm.media.core.control.mgcp.pkg;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.control.mgcp.exception.MalformedMgcpEventRequestException;
import org.restcomm.media.core.control.mgcp.exception.MgcpEventNotFoundException;
import org.restcomm.media.core.control.mgcp.exception.MgcpPackageNotFoundException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GlobalMgcpEventProvider implements MgcpEventProvider {

    private static final Logger log = LogManager.getLogger(GlobalMgcpEventProvider.class);

    private Map<String, MgcpEventProvider> eventProviders;

    public GlobalMgcpEventProvider() {
        this.eventProviders = new HashMap<>(5);
    }

    public void registerProvider(String pkg, MgcpEventProvider provider) {
        final MgcpEventProvider value = this.eventProviders.put(pkg, provider);
        if (value == null && log.isDebugEnabled()) {
            log.debug("Registered MGCP Event Provider for package " + pkg);
        }
    }

    public MgcpEventProvider unregisterProvider(String pkg) {
        final MgcpEventProvider value = this.eventProviders.remove(pkg);
        if (value != null && log.isDebugEnabled()) {
            log.debug("Unregistered MGCP Event Provider for package " + pkg);
        }
        return value;
    }

    @Override
    public MgcpEvent provide(MgcpRequestedEvent event) throws MgcpPackageNotFoundException, MgcpEventNotFoundException, MalformedMgcpEventRequestException {
        final String pkg = event.getPackageName();
        final MgcpEventProvider eventProvider = this.eventProviders.get(pkg);

        if (eventProvider == null) {
            throw new MgcpPackageNotFoundException("No event provider was registered for package " + pkg);
        }
        return eventProvider.provide(event);
    }

}
