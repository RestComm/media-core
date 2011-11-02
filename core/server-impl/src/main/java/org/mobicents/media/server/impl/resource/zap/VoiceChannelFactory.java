/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.impl.resource.zap;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author kulikov
 */
public class VoiceChannelFactory implements ComponentFactory {

    private int span;
    private String name;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setSpan(int span) {
        this.span = span;
    }
    
    public int getSpan() {
        return span;
    }
    
    public Component newInstance(Endpoint endpoint) throws ResourceUnavailableException {
        String endpointName = endpoint.getLocalName();
        System.out.println("local name=" + endpointName);
        String[] tokens = endpointName.split("/");
        
        if (tokens[tokens.length - 1].startsWith("[")) {
            return new VoiceChannel(name, 0);
        }
        int cic = Integer.parseInt(tokens[tokens.length-1]);        
        return new VoiceChannel(name, span);
    }

}
