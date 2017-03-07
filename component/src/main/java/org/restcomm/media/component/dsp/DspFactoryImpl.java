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

package org.restcomm.media.component.dsp;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.dsp.DspFactory;

/**
 * Defines configuration of the DSP and constructs new DSP.
 *
 * @author kulikov
 */
public class DspFactoryImpl implements DspFactory {
	
    //list of registered codecs where codec is represented by its fully qualified class name
    private final ArrayList<String> classes;
    
    public DspFactoryImpl() {
    	this.classes = new ArrayList<String>();
    } 

    /**
     * Registers codec.
     *
     * @param fqn the fully qualified name of codec class.
     */
    public void addCodec(String fqn) {
    	this.classes.add(fqn);
    }

    /**
     * Unregisters codec.
     *
     * @param fqn the fully qualified name of the codec class.
     */
    public void remove(String fqn) {
    	this.classes.remove(fqn);
    }

    /**
     * Creates new DSP.
     *
     * @return DSP instance.
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     */
    @Override
    public Dsp newProcessor() throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        int numClasses = this.classes.size();
    	Codec[] codecs = new Codec[numClasses];
        
        for(int i = 0; i < numClasses; i++) {
        	String fqn = this.classes.get(i);
        	Class<?> codecClass = DspFactoryImpl.class.getClassLoader().loadClass(fqn);
        	codecs[i] = (Codec) codecClass.newInstance();
        }
        return new Dsp(codecs);
    }
    
    @Override
    public void setCodecs(List<String> list) {
        this.classes.addAll(list);
    }
}
