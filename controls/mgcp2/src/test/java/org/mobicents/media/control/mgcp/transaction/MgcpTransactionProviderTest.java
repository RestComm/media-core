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

package org.mobicents.media.control.mgcp.transaction;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionProviderTest {
    
    @Test
    public void testIsTransactionIdLocal() {
        // given
        final int minId = 1;
        final int maxId = 10;
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpTransactionProvider provider = new MgcpTransactionProvider(minId, maxId, commands);

        // when
        boolean minIdIsLocal = provider.isLocal(minId);
        boolean maxIdIsLocal = provider.isLocal(maxId);
        boolean idIsNotLocal = provider.isLocal(maxId + 1);

        // then
        assertTrue(minIdIsLocal);
        assertTrue(maxIdIsLocal);
        assertFalse(idIsNotLocal);
    }
    
    @Test
    public void testProvideLocal() {
        // given
        final int minId = 1;
        final int maxId = 10;
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpTransactionProvider provider = new MgcpTransactionProvider(minId, maxId, commands);

        // when
        List<Integer> identifiers = new ArrayList<>(maxId * 2);
        for (int i = 0; i < maxId * 2; i++) {
            identifiers.add(provider.provideLocal().getId());
        }

        // then
        assertEquals(minId, identifiers.get(minId - 1).intValue());
        assertEquals(maxId, identifiers.get(maxId - 1).intValue());
        assertEquals(minId, identifiers.get(maxId).intValue());
    }
    
    @Test
    public void testProvideRemote() {
        // given
        final int minId = 1;
        final int maxId = 10;
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpTransactionProvider provider = new MgcpTransactionProvider(minId, maxId, commands);
        
        // when
        MgcpTransaction transaction = provider.provideRemote(maxId + 1);
        
        // then
        assertNotNull(transaction);
        assertEquals(maxId + 1, transaction.getId());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testProvideRemoteWithLocalId() {
        // given
        final int minId = 1;
        final int maxId = 10;
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpTransactionProvider provider = new MgcpTransactionProvider(minId, maxId, commands);
        
        // when
        provider.provideRemote(maxId);
    }

}
