/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server;

/**
 *
 * @author kulikov
 */
public class VirtualEndpointImpl extends BaseEndpointImpl {

    public VirtualEndpointImpl(String localName) {
        super(localName);
    }
    
    @Override
    public void unblock() {
    }

    @Override
    public void block() {
    }

}
