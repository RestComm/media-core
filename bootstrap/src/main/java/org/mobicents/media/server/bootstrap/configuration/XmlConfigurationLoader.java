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

package org.mobicents.media.server.bootstrap.configuration;

import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;
import org.mobicents.media.core.configuration.MediaConfiguration;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.core.configuration.MgcpControllerConfiguration;
import org.mobicents.media.core.configuration.MgcpEndpointConfiguration;
import org.mobicents.media.core.configuration.NetworkConfiguration;
import org.mobicents.media.core.configuration.ResourcesConfiguration;

/**
 * Loads Media Server configurations from an XML file.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmlConfigurationLoader implements ConfigurationLoader {
    
    private static final Logger log = Logger.getLogger(XmlConfigurationLoader.class);

    private final Configurations configurations;

    public XmlConfigurationLoader() {
        this.configurations = new Configurations();
    }

    @Override
    public MediaServerConfiguration load(String filepath) {
        // Default configuration
        MediaServerConfiguration configuration = new MediaServerConfiguration();

        // Read configuration from file
        XMLConfiguration xml;
        try {
            xml = this.configurations.xml(filepath);

            // Overwrite default configurations
            configureNetwork(xml.configurationAt("network"), configuration.getNetworkConfiguration());
            configureController(xml.configurationAt("controller"), configuration.getControllerConfiguration());
            configureMedia(xml.configurationAt("media"), configuration.getMediaConfiguration());
            configureResource(xml.configurationAt("resources"), configuration.getResourcesConfiguration());
        } catch (ConfigurationException | IllegalArgumentException e) {
            log.error("Could not load configuration from " + filepath + ". Using default values.");
        }
        return configuration;
    }

    private static void configureNetwork(HierarchicalConfiguration<ImmutableNode> src, NetworkConfiguration dst) {
        dst.setBindAddress(src.getString("bindAddress", "127.0.0.1"));
        dst.setExternalAddress(src.getString("externalAddress", "127.0.0.1"));
        dst.setNetwork(src.getString("network", "127.0.0.1"));
        dst.setSubnet(src.getString("subnet", "255.255.255.255"));
        dst.setSbc(src.getBoolean("sbc", false));
    }

    private static void configureController(HierarchicalConfiguration<ImmutableNode> src, MgcpControllerConfiguration dst) {
        // Basic Controller configuration
        dst.setAddress(src.getString("address", "127.0.0.1"));
        dst.setPort(src.getInt("port", 2427));

        // Iterate over endpoint configuration
        List<HierarchicalConfiguration<ImmutableNode>> endpoints = src.childConfigurationsAt("endpoints");
        for (HierarchicalConfiguration<ImmutableNode> endpoint : endpoints) {
            MgcpEndpointConfiguration endpointConfig = new MgcpEndpointConfiguration();
            endpointConfig.setName(endpoint.getString("[@name]"));
            endpointConfig.setPoolSize(endpoint.getInt("[@poolSize]", 50));
            dst.addEndpoint(endpointConfig);
        }
    }

    private static void configureMedia(HierarchicalConfiguration<ImmutableNode> src, MediaConfiguration dst) {
        // Basic Media configuration
        dst.setTimeout(src.getInt("timeout", 0));
        dst.setLowPort(src.getInt("lowPort", 34534));
        dst.setHighPort(src.getInt("highPort", 65534));
        dst.setJitterBufferSize(src.getInt("jitterBuffer[@size]", 50));

        // Iterate over codec configuration
        List<HierarchicalConfiguration<ImmutableNode>> codecs = src.childConfigurationsAt("codecs");
        for (HierarchicalConfiguration<ImmutableNode> codec : codecs) {
            dst.addCodec(codec.getString("[@name]"));
        }
    }

    private static void configureResource(HierarchicalConfiguration<ImmutableNode> src, ResourcesConfiguration dst) {
        dst.setLocalConnectionCount(src.getInt("localConnection[@poolSize]", 100));
        dst.setRemoteConnectionCount(src.getInt("remoteConnection[@poolSize]", 50));
        dst.setPlayerCount(src.getInt("player[@poolSize]", 50));
        dst.setRecorderCount(src.getInt("recorder[@poolSize]", 50));
        dst.setDtmfDetectorCount(src.getInt("dtmfDetector[@poolSize]", 50));
        dst.setDtmfDetectorDbi(src.getInt("dtmfDetector[@dbi]", -35));
        dst.setDtmfGeneratorCount(src.getInt("dtmfGenerator[@poolSize]", 50));
        dst.setDtmfGeneratorToneVolume(src.getInt("dtmfGenerator[@toneVolume]", -20));
        dst.setDtmfGeneratorToneDuration(src.getInt("dtmfGenerator[@toneDuration]", 80));
        dst.setSignalDetectorCount(src.getInt("signalDetector[@poolSize]", 0));
        dst.setSignalGeneratorCount(src.getInt("signalGenerator[@poolSize]", 0));
    }

}
