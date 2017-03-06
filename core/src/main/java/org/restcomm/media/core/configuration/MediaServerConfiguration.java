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

package org.restcomm.media.core.configuration;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaServerConfiguration {

    private final NetworkConfiguration networkConfiguration;
    private final MgcpControllerConfiguration controllerConfiguration;
    private final MediaConfiguration mediaConfiguration;
    private final ResourcesConfiguration resourcesConfiguration;
    private final DtlsConfiguration dtlsConfiguration;

    public MediaServerConfiguration() {
        this.networkConfiguration = new NetworkConfiguration();
        this.controllerConfiguration = new MgcpControllerConfiguration();
        this.mediaConfiguration = new MediaConfiguration();
        this.resourcesConfiguration = new ResourcesConfiguration();
        this.dtlsConfiguration = new DtlsConfiguration();
    }

    public NetworkConfiguration getNetworkConfiguration() {
        return networkConfiguration;
    }

    public MgcpControllerConfiguration getControllerConfiguration() {
        return controllerConfiguration;
    }

    public MediaConfiguration getMediaConfiguration() {
        return mediaConfiguration;
    }

    public ResourcesConfiguration getResourcesConfiguration() {
        return resourcesConfiguration;
    }

    public DtlsConfiguration getDtlsConfiguration() {
        return dtlsConfiguration;
    }

}
