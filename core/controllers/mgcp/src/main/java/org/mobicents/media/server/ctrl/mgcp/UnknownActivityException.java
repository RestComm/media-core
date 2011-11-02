/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.ctrl.mgcp;

/**
 *
 * @author kulikov
 */
public class UnknownActivityException extends Exception {

    /**
     * Creates a new instance of <code>UnknownActivityException</code> without detail message.
     */
    public UnknownActivityException() {
    }


    /**
     * Constructs an instance of <code>UnknownActivityException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public UnknownActivityException(String msg) {
        super(msg);
    }
}
