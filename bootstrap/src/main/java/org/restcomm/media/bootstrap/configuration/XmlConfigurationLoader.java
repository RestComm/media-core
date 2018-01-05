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

package org.restcomm.media.bootstrap.configuration;

import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.configuration.DriverConfiguration;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.core.configuration.MediaConfiguration;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.MgcpControllerConfiguration;
import org.restcomm.media.core.configuration.MgcpEndpointConfiguration;
import org.restcomm.media.core.configuration.NetworkConfiguration;
import org.restcomm.media.core.configuration.ResourcesConfiguration;
import org.restcomm.media.core.configuration.SubsystemConfiguration;
import org.restcomm.media.core.configuration.SubsystemsConfiguration;

/**
 * Loads Media Server configurations from an XML file.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmlConfigurationLoader implements ConfigurationLoader {
    
    private static final Logger log = LogManager.getLogger(XmlConfigurationLoader.class);
    private static final String MMS_HOME = "mms.home.dir";
    private static final String DEFAULT_PATH = "/conf/mediaserver.xml";

    private final Configurations configurations;

    public XmlConfigurationLoader() {
        this.configurations = new Configurations();
    }

    @Override
    public MediaServerConfiguration load(String filepath) throws Exception {
        // Default configuration
        MediaServerConfiguration configuration = new MediaServerConfiguration();

        // Read configuration from file
        XMLConfiguration xml;
        try {
            // Load from configured path (relative path)
            xml = this.configurations.xml(filepath);
        } catch (ConfigurationException e) {
            log.warn("Could not load configuration from " + filepath);
            // If failed using configured path, try to use default path (absolute path)
            final String mmsHome = System.getProperty(MMS_HOME);
            filepath = mmsHome + DEFAULT_PATH;
            xml = this.configurations.xml(filepath);
            log.warn("Configuration file found at " + filepath);
        }

        // Overwrite default configurations
        configureNetwork(xml.configurationAt("network"), configuration.getNetworkConfiguration());
        configureController(xml.configurationAt("controller"), configuration.getControllerConfiguration());
        configureMedia(xml.configurationAt("media"), configuration.getMediaConfiguration());
        configureResource(xml.configurationAt("resources"), configuration.getResourcesConfiguration());
        configureDtls(xml.configurationAt("dtls"), configuration.getDtlsConfiguration());
        configureSubsystems(xml, configuration.getSubsystemsConfiguration());
        return configuration;
    }

    private static void configureNetwork(HierarchicalConfiguration<ImmutableNode> src, NetworkConfiguration dst) {
        dst.setBindAddress(src.getString("bindAddress", NetworkConfiguration.BIND_ADDRESS));
        dst.setExternalAddress(src.getString("externalAddress", NetworkConfiguration.EXTERNAL_ADDRESS));
        dst.setNetwork(src.getString("network", NetworkConfiguration.NETWORK));
        dst.setSubnet(src.getString("subnet", NetworkConfiguration.SUBNET));
        dst.setSbc(src.getBoolean("sbc", NetworkConfiguration.SBC));
    }

    private static void configureController(HierarchicalConfiguration<ImmutableNode> src, MgcpControllerConfiguration dst) {
        // Basic Controller configuration
        dst.setAddress(src.getString("address", MgcpControllerConfiguration.ADDRESS));
        dst.setPort(src.getInt("port", MgcpControllerConfiguration.PORT));
        dst.setChannelBuffer(src.getInt("channelBuffer", MgcpControllerConfiguration.CHANNEL_BUFFER));

        // Iterate over endpoint configuration
        List<HierarchicalConfiguration<ImmutableNode>> endpoints = src.childConfigurationsAt("endpoints");
        for (HierarchicalConfiguration<ImmutableNode> endpoint : endpoints) {
            MgcpEndpointConfiguration endpointConfig = new MgcpEndpointConfiguration();
            endpointConfig.setName(endpoint.getString("[@name]"));
            endpointConfig.setRelayType(endpoint.getString("[@relay]", "mixer"));
            dst.addEndpoint(endpointConfig);
        }
    }

    private static void configureMedia(HierarchicalConfiguration<ImmutableNode> src, MediaConfiguration dst) {
        // Basic Media configuration
        dst.setMaxDuration(src.getInt("maxDuration", MediaConfiguration.MAX_DURATION));
        dst.setTimeout(src.getInt("timeout", MediaConfiguration.TIMEOUT));
        dst.setLowPort(src.getInt("lowPort", MediaConfiguration.LOW_PORT));
        dst.setHighPort(src.getInt("highPort", MediaConfiguration.HIGH_PORT));
        dst.setJitterBufferSize(src.getInt("jitterBuffer[@size]", MediaConfiguration.JITTER_BUFFER_SIZE));

        // Iterate over codec configuration
        List<HierarchicalConfiguration<ImmutableNode>> codecs = src.childConfigurationsAt("codecs");
        for (HierarchicalConfiguration<ImmutableNode> codec : codecs) {
            dst.addCodec(codec.getString("[@name]"));
        }
    }

    private static void configureResource(HierarchicalConfiguration<ImmutableNode> src, ResourcesConfiguration dst) {
        dst.setDtmfDetectorDbi(src.getInt("dtmfDetector[@dbi]", ResourcesConfiguration.DTMF_DETECTOR_DBI));
        dst.setDtmfDetectorToneDuration(src.getInt("dtmfDetector[@toneDuration]", ResourcesConfiguration.DTMF_DETECTOR_TONE_DURATION));
        dst.setDtmfGeneratorToneVolume(src.getInt("dtmfGenerator[@toneVolume]", ResourcesConfiguration.DTMF_GENERATOR_TONE_VOLUME));
        dst.setDtmfGeneratorToneDuration(src.getInt("dtmfGenerator[@toneDuration]", ResourcesConfiguration.DTMF_GENERATOR_TONE_DURATION));
        dst.setSpeechDetectorSilenceLevel(src.getInt("speechDetector[@silenceLevel]", ResourcesConfiguration.SPEECH_DETECTOR_SILENCE_LEVEL));
        configurePlayer(src, dst);
    }

    private static void configureDtls(HierarchicalConfiguration<ImmutableNode> src, DtlsConfiguration dst){
        dst.setMinVersion(src.getString("minVersion", DtlsConfiguration.MIN_VERSION));
        dst.setMaxVersion(src.getString("maxVersion", DtlsConfiguration.MAX_VERSION));
        dst.setCipherSuites(src.getString("cipherSuites", DtlsConfiguration.CIPHER_SUITES));
        dst.setCertificatePath(src.getString("certificate[@path]", DtlsConfiguration.CERTIFICATE_PATH));
        dst.setKeyPath(src.getString("certificate[@key]", DtlsConfiguration.KEY_PATH));
        dst.setAlgorithmCertificate(src.getString("certificate[@algorithm]", DtlsConfiguration.ALGORITHM_CERTIFICATE));
    }

    private static void configurePlayer(HierarchicalConfiguration<ImmutableNode> src, ResourcesConfiguration dst) {
        HierarchicalConfiguration<ImmutableNode> player = src.configurationAt("player");
        HierarchicalConfiguration<ImmutableNode> cache;
        try {
            cache = player.configurationAt("cache");
        } catch (ConfigurationRuntimeException exception) {
            log.info("No cache was specified for player");
            return;
        }
        dst.setPlayerCache(
                cache.getBoolean("cacheEnabled", ResourcesConfiguration.PLAYER_CACHE_ENABLED),
                cache.getInt("cacheSize", ResourcesConfiguration.PLAYER_CACHE_SIZE)
        );

    }

    private static void configureSubsystems(final XMLConfiguration xml, final SubsystemsConfiguration dst) throws javax.naming.ConfigurationException {
        final HierarchicalConfiguration<ImmutableNode> subsystems;
        try {
            subsystems = xml.configurationAt("subsystems");
        } catch (ConfigurationRuntimeException exception) {
            if(log.isInfoEnabled()) {
                log.info("No subsystems are specified");
            }
            return;
        }
        readSubsystems(subsystems, dst);
    }

    private static void readSubsystems(final HierarchicalConfiguration<ImmutableNode> src, final SubsystemsConfiguration dst) throws javax.naming.ConfigurationException {
        final List<HierarchicalConfiguration<ImmutableNode>> subsystems = src.configurationsAt("subsystem");
        
        if (subsystems == null) {
            return;
        }

        for (final HierarchicalConfiguration<ImmutableNode> subsystem : subsystems) {

            final String subsystemName = subsystem.getString("[@name]");
            final SubsystemConfiguration subsystemConf = new SubsystemConfiguration(subsystemName);
            final List<HierarchicalConfiguration<ImmutableNode>> drivers = subsystem.configurationsAt("driver");
            if (drivers != null) {
                for (final HierarchicalConfiguration<ImmutableNode> driver : drivers) {
                    final String driverName = driver.getString("[@name]");
                    final DriverConfiguration driverConf = new DriverConfiguration(driverName, driver.getString("[@class]"));
                    final List<HierarchicalConfiguration<ImmutableNode>> parameters = driver.configurationsAt("parameter");
                    if (parameters != null) {
                        for (final HierarchicalConfiguration<ImmutableNode> parameter : parameters) {
                            if (parameter != null) {
                                driverConf.addParameter(parameter.getString("[@name]"), parameter.getString("."));
                            }
                        }
                    }
                    subsystemConf.addDriver(driverName, driverConf);
                }
                dst.addSubsystem(subsystemName, subsystemConf);
            }
        }
    }

}
