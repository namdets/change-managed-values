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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

public class InMemoryChangeManagementRepository<T> implements ChangeManagementRepository<T>{
	private ExecutorService workQueue = Executors.newCachedThreadPool();
	private ConcurrentSkipListMap<Long, byte[]> repository = new ConcurrentSkipListMap<Long, byte[]>();
	long timeOfLastStore;

	public InMemoryChangeManagementRepository() {
	}
	public void store(ConcurrentNavigableMap<Long, T> aMap){
		workQueue.execute(new Worker(aMap));
	}

	public ConcurrentNavigableMap<Long, T> retrieve(Long aTime) {
		Entry<Long, byte[]> floorEntry = repository.floorEntry(aTime);
		if(floorEntry==null){
			return null;
		}
		byte[] ceilingValue = floorEntry.getValue();
		if(ceilingValue==null) return null;
		ConcurrentSkipListMap<Long, T> retrieved = getValueFromByteArray(ceilingValue);
		return retrieved;
	}
	private ConcurrentSkipListMap<Long, T> getValueFromByteArray(
			byte[] ceilingValue) {
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(ceilingValue);
		GZIPInputStream zipStream;
		ConcurrentSkipListMap<Long, T> retrieved = null;
		try {
			zipStream = new GZIPInputStream(inBuffer);
			ObjectInputStream objectIn = new ObjectInputStream(zipStream);
			retrieved = (ConcurrentSkipListMap<Long, T>) objectIn.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retrieved;
	}
	private class Worker implements Runnable{
		ConcurrentNavigableMap<Long, T> aMap;
		public Worker(ConcurrentNavigableMap<Long, T> aMap) {
			this.aMap = aMap;
		}

		@Override
		public void run() {
			ByteArrayOutputStream objectBuffer = new ByteArrayOutputStream();
			try {
				objectBuffer = tryToStore();
			} catch (IOException e) {
				run();
			}
			repository.put(aMap.firstKey(), objectBuffer.toByteArray());
		}

		private ByteArrayOutputStream tryToStore() throws IOException {
			ByteArrayOutputStream objectBuffer = new ByteArrayOutputStream();
			GZIPOutputStream zipStream = new GZIPOutputStream(objectBuffer);

			ObjectOutputStream objectBufferStream = new ObjectOutputStream(zipStream);

			objectBufferStream.writeObject(aMap);
			objectBufferStream.close();
			return objectBuffer;
		}

	}
}
