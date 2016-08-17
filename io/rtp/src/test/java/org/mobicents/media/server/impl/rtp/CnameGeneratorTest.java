/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;

/**
 * Tests validity of generated CNAME to be used in RTP sessions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CnameGeneratorTest {
    
    private final static Logger log = Logger.getLogger(CnameGeneratorTest.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public void after() {
        executor.shutdown();
    }

    @Test
    public void testCnameGenerator() {
        // given
        int minCnameSize = 98;
        int capacity = 10000;
        List<String> cnameList = new ArrayList<String>(capacity);

        for (int i = 0; i < capacity; i++) {
            // when
            long start = System.currentTimeMillis();
            String cname = CnameGenerator.generateCname();
            long time = System.currentTimeMillis() - start;
            System.out.println("Took " + time + " ms");

            // then
            // test minimum size
            assertEquals((minCnameSize / 8), Base64.decode(cname).length);
            // test uniqueness
            assertFalse(cnameList.contains(cname));
            cnameList.add(cname);
        }

        // cleanup
        cnameList.clear();
    }

    @Test
    public void testConcurrentCnameGenerator() throws InterruptedException, ExecutionException {
        // given
        GeneratorWorker worker1 = new GeneratorWorker("worker-1");
        GeneratorWorker worker2 = new GeneratorWorker("worker-2");
        GeneratorWorker worker3 = new GeneratorWorker("worker-3");

        // when
        Future<Set<String>> future1 = this.executor.submit(worker1);
        Future<Set<String>> future2 = this.executor.submit(worker2);
        Future<Set<String>> future3 = this.executor.submit(worker3);
        Thread.sleep(3000L);

        // then
        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());
        
        Set<String> cname12 = new HashSet<>(future1.get());
        cname12.retainAll(future2.get());
        assertTrue(cname12.isEmpty());
        
        Set<String> cname13 = new HashSet<>(future1.get());
        cname13.retainAll(future3.get());
        assertTrue(cname13.isEmpty());
        
        Set<String> cname23 = new HashSet<>(future2.get());
        cname23.retainAll(future3.get());
        assertTrue(cname23.isEmpty());
    }

    private class GeneratorWorker implements Callable<Set<String>> {
        
        private final String name;
        
        public GeneratorWorker(String name) {
            this.name = name;
        }

        @Override
        public Set<String> call() throws Exception {
            // given
            int minCnameSize = 98;
            int capacity = 10000;
            Set<String> cnameList = new HashSet<String>(capacity);

            for (int i = 0; i < capacity; i++) {
                // when
                long start = System.currentTimeMillis();
                String cname = CnameGenerator.generateCname();
                long time = System.currentTimeMillis() - start;
                
                log.info(this.name + " took " + time + "ms to generate the cname " + cname);

                // then
                // test minimum size
                assertEquals((minCnameSize / 8), Base64.decode(cname).length);
                // test uniqueness
                assertFalse(cnameList.contains(cname));
                cnameList.add(cname);
            }

            // cleanup
            return cnameList;
        }

    }

}
