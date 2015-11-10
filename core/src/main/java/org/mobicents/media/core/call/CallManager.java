/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.core.call;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CallManager {

    private static final CallManager INSTANCE = new CallManager();

    private final ConcurrentHashMap<Integer, Call> calls;

    private CallManager() {
        this.calls = new ConcurrentHashMap<Integer, Call>();
    }

    public Call getCall(int callId) {
        return this.calls.get(callId);
    }

    public Call createCall(int callId) {
        Call call = new Call(callId);
        Call result = this.calls.putIfAbsent(callId, call);
        return result == null ? call : result;
    }

    public void deleteCall(int callId) {
        Call call = this.calls.remove(callId);
        if (call != null) {
            call.deleteEndpoints();
        }
    }

    public void deleteCalls() {
        Iterator<Integer> iterator = this.calls.keySet().iterator();
        while (iterator.hasNext()) {
            Integer callId = iterator.next();
            deleteCall(callId);
        }
    }

    public static CallManager getInstance() {
        return INSTANCE;
    }

}
