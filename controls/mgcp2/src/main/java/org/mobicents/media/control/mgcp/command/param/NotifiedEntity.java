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

package org.mobicents.media.control.mgcp.command.param;

/**
 * <p>
 * The concept of "notified entity" is central to reliability and fail-over in MGCP.<br>
 * The "notified entity" for an endpoint is the Call Agent currently controlling that endpoint.
 * </p>
 * 
 * <p>
 * At any point in time, an endpoint has one, and only one, "notified entity " associated with it.<br>
 * The "notified entity" determines where the endpoint will send commands to; when the endpoint needs to send a command to the
 * Call Agent, it MUST send the command to its current "notified entity".<br>
 * The "notified entity" however does not determine where commands can be received from; any Call Agent can send commands to the
 * endpoint.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see <a href="https://tools.ietf.org/html/rfc3435#section-2.1.4">RFC3435</a>
 */
public class NotifiedEntity {

    private final String name;
    private final String domain;
    private final int port;
    private final String qualifiedName;

    public NotifiedEntity(String name, String domain, int port) {
        super();
        this.name = name;
        this.domain = domain;
        this.port = port;
        this.qualifiedName = name + "@" + domain + ":" + port;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return this.qualifiedName;
    }
}
