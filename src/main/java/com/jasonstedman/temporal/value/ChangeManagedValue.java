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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.jasonstedman.extensions.ImmutabilityValidator;

public class ChangeManagedValue<T> {
	private static long refNanoTime;
	private static long refMillis;

	static{
		refMillis = System.currentTimeMillis()*1000000;
		refNanoTime = System.nanoTime();
	}

	private int changePointer;
	private ArrayList<Long> changeTimes = new ArrayList<Long>();
	private ChangeManagementRepository<T> repo = new InMemoryChangeManagementRepository<T>();
	private int chunkSize = 1000;
	private int liveChunks = 2;
	private int counter = 0;
	private int changeCount = 0;
	private final ConcurrentSkipListMap<Long, T> temporalMap = 
			new ConcurrentSkipListMap<Long, T> ();
	
	public ChangeManagedValue(T initialValue, int chunkSize){
		if(initialValue==null ||
				!ImmutabilityValidator.isImmutableClass(initialValue.getClass())){
			throw new RuntimeException("Can only manage value changes on immutable classes.");
		}
		update(initialValue);
		this.chunkSize = chunkSize;
	}

	public ChangeManagedValue(T initialValue){
		if(initialValue==null ||
				!ImmutabilityValidator.isImmutableClass(initialValue.getClass())){
			throw new RuntimeException("Can only manage value changes on immutable classes.");
		}
		update(initialValue);
	}

	public void update(T newValue){
		long bigTime = refMillis + (System.nanoTime()-refNanoTime);
		while(temporalMap.putIfAbsent(bigTime, newValue)!=null)bigTime++;
		synchronized(this){
			changeTimes.add(bigTime);
			changePointer = changeCount++;
			counter++;
			if(counter>=chunkSize*liveChunks){
				storeChunk();
				counter=0;
			}
		}
	}
	
	private void storeChunk() {
		Long last = null;
		NavigableSet<Long> keys = temporalMap.keySet();
		ConcurrentSkipListMap<Long, T> chunk = new ConcurrentSkipListMap<Long, T>();
		ArrayList<Long> keyList = new ArrayList<Long>();
		Iterator<Long> i = keys.iterator();
		int ammountOverQuota = temporalMap.size()-(chunkSize*liveChunks);
		for(int x = 0;x<ammountOverQuota;x++){
			last = i.next();
			keyList.add(last);
		}
		for(int x = 0;x<ammountOverQuota;x++){
			chunk.put(keyList.get(x),temporalMap.remove(keyList.get(x)));
		}

		repo.store(chunk);
	}

	public T getValue(){
		return getValue(changeTimes.get(changePointer));
	}

	public List<Long> getChangeTimes(){
		ArrayList<Long> changeTimes = new ArrayList<Long>();
		synchronized(this){
			Collections.sort(this.changeTimes);
		}
		for(Long key : this.changeTimes){
			changeTimes.add(key);
		}
		return changeTimes;
	}

	public T getValue(long changeTime){
		T value = null;
		if(temporalMap.firstKey()>changeTime){
			value = tryToPullFromRepo(changeTime);
		}else{
			ConcurrentNavigableMap<Long, T> valuesPastChangeTime = temporalMap.tailMap(changeTime);
			value = valuesPastChangeTime.firstEntry().getValue();
		}
		return value;
	}

	private T tryToPullFromRepo(long changeTime) {
		ConcurrentNavigableMap<Long, T> retrieve = repo.retrieve(changeTime);
		if(retrieve==null) return null;
		ConcurrentNavigableMap<Long, T> tailMap = retrieve.tailMap(changeTime);
		Entry<Long, T> firstEntry = tailMap.firstEntry();
		return firstEntry.getValue();
	}

	public T undo(){
		if(changePointer>0){
			changePointer--;
		}
		return getValue();
	}

	public T redo(){
		if(changePointer<changeTimes.size()-1){
			changePointer++;
		}
		return getValue();
	}

}
