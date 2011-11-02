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
package org.mobicents.media.server;

import java.util.Map;
import java.util.Set;

import org.mobicents.media.server.resource.ChannelFactory;
import org.mobicents.media.server.spi.MediaType;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public class ConnectionFactory {
	
	private ConnectionStateManager connectionStateManager;
    private ChannelFactory[] rxFactory = new ChannelFactory[2];
    private ChannelFactory[] txFactory = new ChannelFactory[2];
    
    public ChannelFactory[] getRxFactory() {
        return rxFactory;
    }

    public void setRxChannelFactory(Map<String, ChannelFactory> config) {
        if (config != null) {
            define(config, rxFactory);
        }
    }
    
    public ChannelFactory[] getTxFactory() {
        return txFactory;
    }

    public void setTxChannelFactory(Map<String, ChannelFactory> config) {
        if (config != null) {
            define(config, txFactory);
        }
    }

	public ConnectionStateManager getConnectionStateManager() {
		return connectionStateManager;
	}

	public void setConnectionStateManager(
			ConnectionStateManager connectionStateManager) {
		this.connectionStateManager = connectionStateManager;
	}

	private void define(Map<String, ChannelFactory> config, ChannelFactory[] factories) {
        Set<String> names = config.keySet();
        for (String name : names) {
            MediaType mediaType = MediaType.getInstance(name);
            ChannelFactory factory = config.get(name);
            factory.setMediaType(mediaType);
            factories[mediaType.getCode()] = factory;
        }
    }
}
