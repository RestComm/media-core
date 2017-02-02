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

package org.mobicents.media.control.mgcp.call;

import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GlobalMgcpCallManagerTest {

    @Test
    public void testRegisterCall() {
        // given
        final MgcpCall call1 = mock(MgcpCall.class);
        final MgcpCall call2 = mock(MgcpCall.class);
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        when(call1.getCallId()).thenReturn(1);
        when(call1.getCallIdHex()).thenReturn("1");

        when(call2.getCallId()).thenReturn(2);
        when(call2.getCallIdHex()).thenReturn("2");

        final boolean isRegistered = callManager.registerCall(call1);
        final MgcpCall registeredCall = callManager.getCall(call1.getCallId());
        final MgcpCall nonRegisteredCall = callManager.getCall(call2.getCallId());

        // then
        assertTrue(isRegistered);
        assertEquals(call1, registeredCall);
        assertNull(nonRegisteredCall);
    }

    @Test
    public void testRegisterDuplicateCall() {
        // given
        final MgcpCall call = mock(MgcpCall.class);
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        when(call.getCallId()).thenReturn(1);
        when(call.getCallIdHex()).thenReturn("1");

        final boolean isRegistered1 = callManager.registerCall(call);
        final boolean isRegistered2 = callManager.registerCall(call);

        // then
        assertTrue(isRegistered1);
        assertFalse(isRegistered2);
    }

    @Test
    public void testUnregisterCall() {
        // given
        final MgcpCall call = mock(MgcpCall.class);
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        when(call.getCallId()).thenReturn(1);
        when(call.getCallIdHex()).thenReturn("1");

        final boolean isRegistered = callManager.registerCall(call);
        final MgcpCall unregisteredCall = callManager.unregisterCall(call.getCallId());
        final MgcpCall retirevedCall = callManager.getCall(call.getCallId());

        // then
        assertTrue(isRegistered);
        assertEquals(call, unregisteredCall);
        assertNull(retirevedCall);
    }

    @Test
    public void testUnregisterInexistentCall() {
        // given
        final MgcpCall call = mock(MgcpCall.class);
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        when(call.getCallId()).thenReturn(1);
        when(call.getCallIdHex()).thenReturn("1");

        final boolean isRegistered = callManager.registerCall(call);
        final MgcpCall unregisteredCall1 = callManager.unregisterCall(call.getCallId());
        final MgcpCall unregisteredCall2 = callManager.unregisterCall(call.getCallId());

        // then
        assertTrue(isRegistered);
        assertEquals(call, unregisteredCall1);
        assertNull(unregisteredCall2);
    }

    @Test
    public void testUnregisterCalls() {
        // given
        final MgcpCall call1 = mock(MgcpCall.class);
        final MgcpCall call2 = mock(MgcpCall.class);
        final MgcpCall call3 = mock(MgcpCall.class);
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        when(call1.getCallId()).thenReturn(1);
        when(call1.getCallIdHex()).thenReturn("1");

        when(call2.getCallId()).thenReturn(2);
        when(call2.getCallIdHex()).thenReturn("2");

        when(call3.getCallId()).thenReturn(3);
        when(call3.getCallIdHex()).thenReturn("3");

        callManager.registerCall(call1);
        callManager.registerCall(call2);
        callManager.registerCall(call3);

        final Set<MgcpCall> unregistered = callManager.unregisterCalls();

        final MgcpCall retrievedCall1 = callManager.getCall(call1.getCallId());
        final MgcpCall retrievedCall2 = callManager.getCall(call2.getCallId());
        final MgcpCall retrievedCall3 = callManager.getCall(call3.getCallId());

        // then
        assertEquals(3, unregistered.size());
        assertTrue(unregistered.contains(call1));
        assertTrue(unregistered.contains(call2));
        assertTrue(unregistered.contains(call3));
        assertNull(retrievedCall1);
        assertNull(retrievedCall2);
        assertNull(retrievedCall3);
    }

    @Test
    public void testUnregisterCallsFromEmptyCallManager() {
        // given
        final MgcpCallManager callManager = new GlobalMgcpCallManager();

        // when
        final Set<MgcpCall> unregistered = callManager.unregisterCalls();

        // then
        assertNotNull(unregistered);
        assertTrue(unregistered.isEmpty());
    }

}
