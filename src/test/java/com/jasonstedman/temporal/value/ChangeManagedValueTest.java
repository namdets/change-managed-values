/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Jason Stedman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.jasonstedman.temporal.value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ChangeManagedValueTest 
extends TestCase
{
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public ChangeManagedValueTest( String testName )
	{
		super( testName );
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( ChangeManagedValueTest.class );
	}

	public void testChangeManagedValue()
	{
		// Create a ChangeManagedValue to pump values into.
		ChangeManagedValue<Integer> v = new ChangeManagedValue<Integer>(0, 3000);
		// Ensure there are the proper number of values
		assertEquals(true, v.getChangeTimes().size()==1);
		// Ensure the value inserted is the value retrieved
		assertEquals(0, v.getValue().intValue());
		// Fill it up with some more values
		System.out.println("value : " + 0 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(1);
		System.out.println("value : " + 1 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(2);
		System.out.println("value : " + 2 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(3);
		System.out.println("value : " + 3 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(4);
		System.out.println("value : " + 4 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(5);
		System.out.println("value : " + 5 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		v.update(6);
		System.out.println("value : " + 6 + " changeTime : " + v.getChangeTimes().get(v.getChangeTimes().size()-1));
		// Ensure there are still the proper number of values
		assertEquals(true, v.getChangeTimes().size()==7);
		// Ensure the values are in the correct order
		long lastChangeTime = 0;
		int lastValue = -1;
		for(long changeTime : v.getChangeTimes()){
			assertTrue(changeTime>lastChangeTime);
			Integer value = v.getValue(changeTime);
			System.out.println("value : " + value + "  lastValue : " + lastValue + "  changeTime : " + changeTime);
			assertTrue(value-lastValue==1);
			lastChangeTime = changeTime;
			lastValue = value;
		}
		// Ensure that undo backs up exactly one change
		int value = v.getValue();
		v.undo();
		assertTrue(value-v.getValue()==1);
		// Ensure that undo 6 times gets us back to zero.
		for(int x = 0;x<6;x++) v.undo();
		assertTrue(v.getValue()==0);
		// Ensure that another undo keeps us at zero and does not result in a null.
		v.undo();
		assertTrue(v.getValue()==0);
		// Ensure that 6 redos puts us at 6
		for(int x = 0;x<6;x++) v.redo();
		assertTrue(v.getValue()==6);
		// Ensure that further redos doesnt cause issues and leaves us a 6
		v.redo();
		assertTrue(v.getValue()==6);
		// Ensure the values are still in the correct order
		lastChangeTime = 0;
		lastValue = -1;
		for(long changeTime : v.getChangeTimes()){
			assertTrue(changeTime>lastChangeTime);
			value = v.getValue(changeTime);
			System.out.println("value : " + value + "  lastValue : " + lastValue + "  changeTime : " + changeTime);
			assertTrue(value-lastValue==1);
			lastChangeTime = changeTime;
			lastValue = value;
		}
		int size = 1000;
		offliningTestCase(size);
	}
	
	private void offliningTestCase(int size) {
		long startTime = System.currentTimeMillis();
				
		ChangeManagedValue<Long> v;
		v = new ChangeManagedValue<Long>(0l, size);

		ExecutorService ex = Executors.newFixedThreadPool(10);
		for(int x = 0;x<10;x++) {
			ex.execute(new TestThread(v));
		}
		ex.shutdown();
		try {
			while(!ex.awaitTermination(100, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while(!ex.isShutdown());

		System.out.println("Data Load");
		System.out.println("Time : " +(System.currentTimeMillis()-startTime));
		List<Long> changeTimes = v.getChangeTimes();
		System.out.println("Size : " +changeTimes.size());
		
		// Test retrieval of values at 1000000 random times

		ArrayList<Long> retrievalTimes = new ArrayList<Long>();
		for(int x = 0;x<1000;x++){
			retrievalTimes.add(changeTimes.get((int)(Math.random()*changeTimes.size())));
		}
		
		System.out.println("Random Retrieval of 1K items");
		startTime = System.currentTimeMillis();
		for(int x = 0;x<1000;x++){
			v.getValue(retrievalTimes.get(x));
		}
		System.out.println("Time : " +(System.currentTimeMillis()-startTime));
		
	}

	private class TestThread implements Runnable {
		ChangeManagedValue<Long> value;
		public TestThread(ChangeManagedValue<Long> value){
			this.value = value;
		}

		public void run() {
				for(int x = 0;x<100000;x++){
					value.update((long)(Math.random()*10000000000000l));
					Thread.yield();
				}
		}

	}
}
