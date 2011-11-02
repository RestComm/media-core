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
package org.mobicents.media.server.impl.resource.dtmf;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author amit bhayani
 *
 */
public class GeneratorFactory implements ComponentFactory {

    private String name;
    private int duration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the minum length of generated tone.
     * 
     * @return tone duration in milliseconds.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Modify minim duration of generated tone.
     * 
     * @param duration the duration of the tone in milliseconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Component newInstance(Endpoint endpoint) throws ResourceUnavailableException {
        GeneratorImpl generator = new GeneratorImpl(this.name);
        generator.setEndpoint(endpoint);
        generator.setToneDuration(duration);
        return generator;
    }
}
