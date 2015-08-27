/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.core.endpoints.impl;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.endpoints.AbstractRelayEndpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;

/**
 * Announcement endpoint implementation
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AnnouncementEndpoint extends AbstractRelayEndpoint {

    private static final Logger logger = Logger.getLogger(AnnouncementEndpoint.class);

    public AnnouncementEndpoint(String localName, RelayType relayType) {
        super(localName, relayType);
    }

    public AnnouncementEndpoint(String localName) {
        super(localName, RelayType.MIXER);
    }

    @Override
    public Component getResource(MediaType mediaType, ComponentType componentType) {
        switch (mediaType) {
            case AUDIO:
                switch (componentType) {
                    case PLAYER:
                        return mediaGroup.getPlayer();
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
