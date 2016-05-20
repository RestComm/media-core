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

package org.mobicents.media.control.mgcp.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.recorder.Recorder;

/**
 * Manages a group of media resources that will be used throughout the call.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaGroup {

    private final Map<ComponentType, Component> components;
    private final AudioComponent inbandComponent;
    private final OOBComponent outbandComponent;

    public MediaGroup(AudioComponent inbandComponent, OOBComponent outbandComponent) {
        this.components = new HashMap<>(ComponentType.values().length);
        this.inbandComponent = inbandComponent;
        this.outbandComponent = outbandComponent;
    }

    ComponentType resolveType(Component component) {
        if (component instanceof Player) {
            return ComponentType.PLAYER;
        }
        if (component instanceof Recorder) {
            return ComponentType.RECORDER;
        }
        if (component instanceof Player) {
            return ComponentType.DTMF_DETECTOR;
        }
        if (component instanceof Player) {
            return ComponentType.DTMF_GENERATOR;
        }
        if (component instanceof Player) {
            return ComponentType.SINE;
        }
        if (component instanceof Player) {
            return ComponentType.SPECTRA_ANALYZER;
        }
        return null;
    }

    private void registerComponent(ComponentType type, Component component) {
        switch (type) {
            case PLAYER:
                this.inbandComponent.addInput(((AudioPlayerImpl) component).getAudioInput());
                break;

            case RECORDER:
                this.inbandComponent.addOutput(((AudioRecorderImpl) component).getAudioOutput());
                break;

            case DTMF_DETECTOR:
                this.inbandComponent.addOutput(((DetectorImpl) component).getAudioOutput());
                this.outbandComponent.addOutput(((DetectorImpl) component).getOOBOutput());
                break;

            case DTMF_GENERATOR:
                this.inbandComponent.addInput(((GeneratorImpl) component).getAudioInput());
                this.outbandComponent.addInput(((GeneratorImpl) component).getOOBInput());
                break;
            default:
                break;
        }
    }

    private void unregisterComponent(ComponentType type, Component component) {
        switch (type) {
            case PLAYER:
                this.inbandComponent.remove(((AudioPlayerImpl) component).getAudioInput());
                break;

            case RECORDER:
                this.inbandComponent.remove(((AudioRecorderImpl) component).getAudioOutput());
                // TODO update audio component status
                break;

            case DTMF_DETECTOR:
                this.inbandComponent.remove(((DetectorImpl) component).getAudioOutput());
                this.outbandComponent.remove(((DetectorImpl) component).getOOBOutput());
                break;

            case DTMF_GENERATOR:
                this.inbandComponent.remove(((GeneratorImpl) component).getAudioInput());
                this.outbandComponent.remove(((GeneratorImpl) component).getOOBInput());
                break;
            default:
                throw new IllegalArgumentException("Component " + type.name() + " not supported.");
        }
    }

    /**
     * Registers a new component in the Media Group.
     * 
     * @param component The component to be registered
     * @throws IllegalArgumentException If a component of same type is already registered
     */
    public void addComponent(Component component) throws IllegalArgumentException {
        // Resolve component type
        final ComponentType type = resolveType(component);

        // Validate component
        if (type == null) {
            throw new IllegalArgumentException("Unsupported component type");
        }

        if (this.components.containsKey(type)) {
            throw new IllegalArgumentException("There is already a " + type + " component registered.");
        }

        // Register and setup component
        this.components.put(type, component);
        registerComponent(type, component);
    }

    /**
     * Removes a registered component.
     * 
     * @param type The component type.
     * @return The unregistered component. Returns null if no such component exists.
     */
    public Component removeComponent(ComponentType type) {
        final Component component = this.components.remove(type);
        if (component != null) {
            unregisterComponent(type, component);
        }
        return component;
    }

    /**
     * Removes all registered components.
     * 
     * @return A list containing all registered components. The list will be empty if no components exist.
     */
    public List<Component> removeComponents() {
        ArrayList<Component> copy = new ArrayList<>(this.components.values());
        this.components.clear();
        return copy;
    }

    /**
     * Gets a registered components.
     * 
     * @param type The component type
     * @return The registered component. Returns null if no such component exists.
     */
    public Component getComponent(ComponentType type) {
        return this.components.get(type);
    }

}
