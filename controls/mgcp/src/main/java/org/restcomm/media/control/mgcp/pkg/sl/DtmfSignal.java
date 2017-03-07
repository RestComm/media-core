/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
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
package org.restcomm.media.control.mgcp.pkg.sl;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author Yulian Oifa
 */
public final class DtmfSignal 
{
		public static final String TONE_0="0";
		public static final String TONE_1="1";
		public static final String TONE_2="2";
		public static final String TONE_3="3";
		public static final String TONE_4="4";
		public static final String TONE_5="5";
		public static final String TONE_6="6";
		public static final String TONE_7="7";
		public static final String TONE_8="8";
		public static final String TONE_9="9";
		public static final String TONE_A="A";
		public static final String TONE_B="B";
		public static final String TONE_C="C";
		public static final String TONE_D="D";
		public static final String TONE_HASH="#";
		public static final String TONE_STAR="*";
		
		public static String[] ALL_TONES={ TONE_0, TONE_1 , TONE_2, TONE_3, TONE_4, TONE_5, TONE_6,
			TONE_7,TONE_8,TONE_9,TONE_A,TONE_B,TONE_C,TONE_D,TONE_HASH,TONE_STAR };
		
		public static final int DEFAULT_DURATION=200;
		
        private String dg;
        private int to;
        
        public DtmfSignal(String dg, int to) 
        {
                this.dg = dg;
                this.to = to;
        }
        
        public DtmfSignal(String dg) 
        {
                this(dg, 200);
        }
        
        public String getDigit() 
        {
                return dg;
        }
        
        public int getDuration() {
                return to;
        }
        
        public void setDigit(final String dg) 
        {
                this.dg = dg;
        }
        
        public void setDuration(final int to) 
        {
                this.to = to;
        }
}