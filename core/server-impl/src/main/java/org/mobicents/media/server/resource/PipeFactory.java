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

package org.mobicents.media.server.resource;

import org.mobicents.media.server.spi.Valve;

/**
 * Used as a factory class for creating inner pipes dynamic upon request
 * using assigned inlet's and outlet's factrories;
 * 
 * @author kulikov
 */
public class PipeFactory {
    
    private String inlet;
    private String outlet;
    private Valve valve;
    
    public String getInlet() {
        return inlet;
    }

    public String getOutlet() {
        return outlet;
    }

    public void setInlet(String inlet) {
        this.inlet = inlet;
    }

    public void setOutlet(String outlet) {
        this.outlet = outlet;
    }

    public Valve getValve() {
        return valve;
    }
    
    public void setValve(Valve valve) {
        this.valve = valve;
    }
    
    public void openPipe(Channel channel) throws UnknownComponentException {
        Pipe pipe = new Pipe(channel);
        pipe.setValve(this.valve);
        channel.openPipe(pipe, inlet, outlet);
    }
}
