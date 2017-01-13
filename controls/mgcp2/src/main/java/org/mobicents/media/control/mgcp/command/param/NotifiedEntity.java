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
 * <p>
 * Upon startup, the "notified entity" MUST be set to a provisioned value.<br>
 * Most commands sent by the Call Agent include the ability to explicitly name the "notified entity" through the use of a
 * "NotifiedEntity" parameter.<br>
 * The "notified entity" will stay the same until either a new "NotifiedEntity" parameter is received or the endpoint does a
 * warm or cold (power-cycle) restart.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see <a href="https://tools.ietf.org/html/rfc3435#section-2.1.4">RFC3435</a>
 */
public class NotifiedEntity {

    private static final String DEFAULT_NAME = "call-agent";
    private static final String DEFAULT_DOMAIN = "127.0.0.1";
    private static final int DEFAULT_PORT = 2727;

    private String name;
    private String domain;
    private int port;
    private final StringBuilder builder;

    public NotifiedEntity(String name, String domain) {
        this(name, domain, 0);
    }

    public NotifiedEntity(String name, String domain, int port) {
        super();
        this.name = name;
        this.domain = domain;
        this.port = port;
        this.builder = new StringBuilder();
    }

    public NotifiedEntity() {
        this(DEFAULT_NAME, DEFAULT_DOMAIN, DEFAULT_PORT);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        
        if(obj instanceof NotifiedEntity) {
            NotifiedEntity other = (NotifiedEntity) obj;
            return this.name.equals(other.name) && this.domain.equals(other.domain) && this.port == other.port;
        }
        return false;
    }

    @Override
    public String toString() {
        this.builder.setLength(0);
        this.builder.append(getName()).append("@").append(getDomain());
        if(this.port != 0) {
            this.builder.append(":").append(getPort());
        }
        return this.builder.toString();
    }
}
