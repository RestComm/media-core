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

package org.mobicents.javax.media.mscontrol;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;

import junit.framework.TestCase;

public class AbstractJoinableTest extends TestCase {

	public AbstractJoinableTest() {
		super();
	}

	public AbstractJoinableTest(String name) {
		super(name);
	}

	public void setUp() {

	}

	public void tearDown() throws Exception {

	}

	public void testAbstarctJoinable() {
		TestAbstractJoinable j1 = new TestAbstractJoinable();
		TestAbstractJoinable j2 = new TestAbstractJoinable();
		TestAbstractJoinable j3 = new TestAbstractJoinable();

		// J1 Sends to J2
		try {
			j1.join(Joinable.Direction.SEND, j2);
			assertEquals(Joinable.Direction.RECV, j2.getDirection());
		} catch (MsControlException e) {
			// Un-expected
			e.printStackTrace();
			fail("The join operation failed ");
		}

		// J2 SEND_RECV from J3 which should fail
		try {
			j2.join(Joinable.Direction.DUPLEX, j3);
			fail("J2 already conneted to J1 hance fails");
		} catch (MsControlException e) {
			// Expected
		}

		try {
			j1.unjoin(j2);
		} catch (MsControlException e) {
			// un-expected
			e.printStackTrace();
			fail();
		}

		// Now J2 can join J3
		try {
			j2.join(Joinable.Direction.DUPLEX, j3);
			assertEquals(Joinable.Direction.DUPLEX, j3.getDirection());
		} catch (MsControlException e) {
			// Un-expected
			e.printStackTrace();
			fail("Joining J2 failed with J3");
		}

	}

	private class TestAbstractJoinable extends AbstractJoinable {

	}

}
