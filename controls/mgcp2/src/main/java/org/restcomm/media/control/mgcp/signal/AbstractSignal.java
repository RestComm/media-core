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

package org.restcomm.media.control.mgcp.signal;

import com.google.common.util.concurrent.FutureCallback;

import java.util.Map;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 03/10/2017
 */
public abstract class AbstractSignal<T> implements MgcpSignal<T> {

    private final String requestId;
    private final String pkg;
    private final String symbol;
    private final Map<String, String> parameters;

    public AbstractSignal(String requestId, String pkg, String symbol, Map<String, String> parameters) {
        this.requestId = requestId;
        this.pkg = pkg;
        this.symbol = symbol;
        this.parameters = parameters;
    }

    public String getPackage() {
        return pkg;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    @Override
    public String getRequestId() {
        return this.requestId;
    }

    @Override
    public abstract void execute(FutureCallback<T> callback);

    @Override
    public String toString() {
        return this.pkg + "/" + this.symbol + " (X:" + this.requestId + ")";
    }

}
