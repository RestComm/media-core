/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.mobicents.media.server.spi.events;

import java.io.Serializable;
import org.mobicents.media.Component;

/**
 *
 * @author Oleg Kulikov
 */
public interface NotifyEvent extends Serializable {
    public final static int STARTED = 10000;
    public final static int COMPLETED = 20000;
    public final static int STOPPED = 30000;

    public final static int START_FAILED = 10001;
    public final static int TX_FAILED = 10002;
    public final static int RX_FAILED = 10003;
    
    public Component getSource();
    public int getEventID();
}
