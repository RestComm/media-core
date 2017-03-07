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

package org.restcomm.media.control.mgcp.transaction;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionNumberspace;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionNumberspaceTest {

    @Test()
    public void testDefaultRange() {
        // given
        final int minimumDefault = 1;
        final int maximumDefault = 999999999;

        // when
        final MgcpTransactionNumberspace numberspace = new MgcpTransactionNumberspace();

        // then
        assertEquals(minimumDefault, numberspace.getMinimum());
        assertEquals(maximumDefault, numberspace.getMaximum());
    }

    @Test()
    public void testValidRange() {
        // given
        final int minimum = 1000;
        final int maximum = 999999998;

        // when
        final MgcpTransactionNumberspace numberspace = new MgcpTransactionNumberspace(minimum, maximum);

        // then
        assertEquals(minimum, numberspace.getMinimum());
        assertEquals(maximum, numberspace.getMaximum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumOutsideScope() {
        // given
        final int minimum = 0;
        final int maximum = 1000;

        // when
        new MgcpTransactionNumberspace(minimum, maximum);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumOutsideScope() {
        // given
        final int minimum = 1000;
        final int maximum = 1000000000;

        // when
        new MgcpTransactionNumberspace(minimum, maximum);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumLesserThanMinimum() {
        // given
        final int minimum = 2000;
        final int maximum = 1000;

        // when
        new MgcpTransactionNumberspace(minimum, maximum);
    }

    @Test()
    public void testNoConcurrentCollisions() throws Exception {
        // given
        final int minimum = 1;
        final int maximum = 999;
        final MgcpTransactionNumberspace numberspace = new MgcpTransactionNumberspace(minimum, maximum);

        final NumberspaceConsumer consumer1 = new NumberspaceConsumer(numberspace, maximum / 3);
        final NumberspaceConsumer consumer2 = new NumberspaceConsumer(numberspace, maximum / 3);
        final NumberspaceConsumer consumer3 = new NumberspaceConsumer(numberspace, maximum / 3);

        // when
        new Thread(consumer1).start();
        new Thread(consumer2).start();
        new Thread(consumer3).start();
        Thread.sleep(1000);

        // then
        final Set<Integer> uniques = new HashSet<>(maximum);
        assertFalse(existDuplicate(consumer1.numbers, uniques));
        assertFalse(existDuplicate(consumer2.numbers, uniques));
        assertFalse(existDuplicate(consumer3.numbers, uniques));
        assertEquals(minimum, numberspace.generateId());
    }

    private boolean existDuplicate(Collection<Integer> source, Set<Integer> existing) {
        for (Integer digit : source) {
            boolean added = existing.add(digit);
            if (!added) {
                return true;
            }
        }
        return false;
    }

    private final class NumberspaceConsumer implements Runnable {

        private final MgcpTransactionNumberspace numberspace;
        private final List<Integer> numbers;
        private final int runs;

        public NumberspaceConsumer(MgcpTransactionNumberspace numberspace, int runs) {
            this.numberspace = numberspace;
            this.numbers = new ArrayList<>(runs);
            this.runs = runs;
        }

        @Override
        public void run() {
            for (int i = 0; i < runs; i++) {
                int next = numberspace.generateId();
                this.numbers.add(next);
            }
        }
    }

}
