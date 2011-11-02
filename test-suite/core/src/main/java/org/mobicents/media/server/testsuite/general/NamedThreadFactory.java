/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.testsuite.general;

import java.util.concurrent.ThreadFactory;

/**
 *
 * @author baranowb
 */
public class NamedThreadFactory implements ThreadFactory{

    
    private final static ThreadGroup tg = new ThreadGroup("MMS-Test-Tool-TG");
    private final String factoryName;
    
    public NamedThreadFactory(String name)
    {
        this.factoryName = name;
    }
    
    public Thread newThread(Runnable r) {
       
        Thread t = new Thread(tg, r ,this.factoryName);
        return t;
    }

}
