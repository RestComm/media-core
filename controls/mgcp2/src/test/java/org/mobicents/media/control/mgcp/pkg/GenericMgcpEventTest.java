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
        
package org.mobicents.media.control.mgcp.pkg;

import static org.mockito.Mockito.*;

import org.junit.Test;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEventTest {
    
    @Test
    public void testFire() {
        // given
        MgcpEventListener listener1 = mock(MgcpEventListener.class);
        MgcpEventListener listener2 = mock(MgcpEventListener.class);
        MgcpEventListener listener3 = mock(MgcpEventListener.class);
        GenericMgcpEvent event = new GenericMgcpEvent("AU", "evt", "pa");
        
        // when
        event.fire(listener1, listener2, listener3);
        
        // then
        verify(listener1, times(1)).onMgcpEvent(event);
        verify(listener2, times(1)).onMgcpEvent(event);
        verify(listener3, times(1)).onMgcpEvent(event);
    }

    @Test(expected=IllegalStateException.class)
    public void testDoubleFire() {
        // given
        MgcpEventListener listener1 = mock(MgcpEventListener.class);
        MgcpEventListener listener2 = mock(MgcpEventListener.class);
        MgcpEventListener listener3 = mock(MgcpEventListener.class);
        GenericMgcpEvent event = new GenericMgcpEvent("AU", "evt", "pa");
        
        // when
        event.fire(listener1, listener2, listener3);
        event.fire(listener1, listener2, listener3);
    }

}
