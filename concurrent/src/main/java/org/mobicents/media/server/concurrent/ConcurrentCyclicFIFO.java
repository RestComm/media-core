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

package org.mobicents.media.server.concurrent;

import java.util.Iterator;
import java.util.Queue;
import java.util.Collection;
import java.lang.UnsupportedOperationException;
import java.util.NoSuchElementException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;
/**
 *
 * @author oifa yulian
 */
public class ConcurrentCyclicFIFO<E> {
	//main list
	private Node<E> head;
	private Node<E> tail;
	
	private Integer size=0;
	private Integer availableSize=0;
	
	//notifiers
	private Notifier notifierHead;
	private Notifier notifierTail;
	
	private Integer notifierSize=0;
	private Integer availableNotifierSize=0;
	
	private Integer lastId=1;
	private Lock lock=new Lock();
	
	public ConcurrentCyclicFIFO()
	{
		head=new Node<E>(lastId++);		
		head.next=head;
		tail=head;
		
		notifierHead=new Notifier();
		notifierHead.next=notifierHead;		
		notifierTail=notifierHead;
	}
	
	public boolean offer(E value)
	{
		if(value==null)
			return false;
		
		aquireAccess();		
		
		if(notifierSize>0)
		{
			notifierSize--;
			availableNotifierSize++;	    	
	    	Notifier tempNotifier=notifierHead;
			notifierHead=notifierHead.next;
			tempNotifier.waitingThread.setValue(value);		
			LockSupport.unpark(tempNotifier.waitingThread);
			
			releaseAccess();
			return true;
		}
		
		if(availableSize==0)
		{
			//need new node
			Node<E> tempNode=new Node(lastId++);
			tempNode.next=head;
			
			tail.next=tempNode;
			tail.element=value;
			
			tail=tempNode;
		}
		else
		{
			//obtain node from cache decrease cache size
			tail.element=value;			
			tail=tail.next;			
			availableSize--;
		}
		
		size++;
		
		releaseAccess();
		return true;
	}
	
	public E peek()
	{
		aquireAccess();
				
		E currValue=null;
		if(size>0)
			currValue=head.element;
		
		releaseAccess();		
		return currValue;
	}
	
	public E poll()
	{
		aquireAccess();
		
		if(size==0)
		{
			releaseAccess();
			return null;
		}
		
		E currValue=head.element;
		head=head.next;
		
		//update counters
		availableSize++;
		size--;
		
		releaseAccess();
		return currValue;
	}
	
	public E take()
	{
		aquireAccess();
		
		if(size==0)
		{			
			WrappedThread currThread=(WrappedThread)Thread.currentThread();
			currThread.setValue(null);
			
			if(availableNotifierSize==0)
			{
				//need new node
				Notifier<E> tempNotifier=new Notifier();
				tempNotifier.next=notifierHead;				
				notifierTail.next=tempNotifier;
				notifierTail.waitingThread=currThread;		
				notifierTail=tempNotifier;
			}
			else
			{
				notifierTail.waitingThread=currThread;				
				notifierTail=notifierTail.next;
				availableNotifierSize--;
			}

			notifierSize++;			
			releaseAccess();
		
			E result=(E)currThread.getValue();
			while(result==null)
			{
				LockSupport.park();
				result=(E)currThread.getValue();				
			}
			
			return result;
		}
		
		E currValue=head.element;
		head=head.next;
		
		availableSize++;
		size--;
		
		releaseAccess();
		return currValue;
	}
	
	public void clear()
	{
		aquireAccess();
		
		if(size>0)
		{
			availableSize+=size;
			size=0;			
			head=tail;			
		}
		
		releaseAccess();
	}
	
	public boolean isEmpty()
	{
		return size==0;
	}
	
	public int size()
	{
		return size;
	}
	
	public Iterator<E> iterator()
	{
		return new ListIterator();		
	}
	
	public void resetIterator(Iterator<E> iterator)
	{
		((ListIterator)iterator).currNode=head;
	}
	
	private void aquireAccess()
    {
    	try
    	{
    		lock.lock();
    	}
    	catch(InterruptedException e)
    	{
    		
    	}
    }
    
    private void releaseAccess()
    {
    	lock.unlock();    	
    }
    
    private class Notifier<E> 
    {
    	Notifier next;    	
    	WrappedThread waitingThread;
    }
    
    private class Node<E> 
    {
    	E element;
    	Node<E> next;
    	
    	int id;    	
    	
    	Node(int id)
    	{    		
    		this.id=id;
    	}    	    	   	   
    }
    
    private class ListIterator implements Iterator<E>
    {
    	private Node<E> currNode;
    	
    	public ListIterator()
    	{
    		this.currNode=head;
    	}
    	
    	public boolean hasNext()
    	{
    		return currNode.id!=tail.id;
    	}
    	
    	public E next()
    	{
    		E  result=currNode.element;
    		currNode=currNode.next;
    		return result;
    	}
    	
    	public void remove() throws UnsupportedOperationException
    	{
    		throw new UnsupportedOperationException();
    	}
    }
}
