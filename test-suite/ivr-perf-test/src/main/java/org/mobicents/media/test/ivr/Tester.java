/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.test.ivr;

import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpStack;
import java.net.InetAddress;

import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.concurrent.Semaphore;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
/**
 *
 * @author kulikov
 */
public class Tester {

    public final static int N = 100;
    public final static String ADDRESS = "192.168.1.2";
    public final static int port = 2828;
    
    private JainMgcpStack stack;
    private JainMgcpProvider provider;
    
    private HashMap<Integer, Call> calls = new HashMap();
    private Semaphore semaphore = new Semaphore(0);
    
    private String report = "-------------------------------------\n";
    private int failureCount;
    
    public Tester() throws Exception {
        InetAddress address = InetAddress.getByName(ADDRESS);
	stack = new JainMgcpStackImpl(address, port);
        provider = stack.createProvider();
    }
    
    private void close() throws DeleteProviderException {
        stack.deleteProvider(provider);
        Call.timer.purge();
        Call.timer.cancel();
        System.out.println("DONE");
    }
    
    private void runTest() throws TooManyListenersException, InterruptedException, DeleteProviderException {
        for (int i = 0; i < N; i++) {
            Call call = new Call(this, provider, 180);
            calls.put(call.getID(), call);
            call.setup();
            Thread.currentThread().sleep(1000);
        }
        semaphore.acquire(N);
        System.out.println(report);
        System.out.println("------------------------------");
        System.out.println("Failed = " + failureCount);
        close();
    }
    
    protected void removeCall(Call call) {
        if (calls.containsKey(call.getID())) {
            calls.remove(call.getID());
            if (!call.isFailed()) {
                report += call.getReport() + "\n";
            } else {
                failureCount++;
            }
            semaphore.release();
        }
    }
    
    public static void main(String[] args) throws Exception {
        Tester tester = new Tester();
        tester.runTest();
//        tester.close();
    }
}
