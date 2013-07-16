package com.jasonstedman.temporal.value;

import java.io.IOException;
import java.util.concurrent.ConcurrentNavigableMap;

public interface ChangeManagementRepository<T> {
	public void store(ConcurrentNavigableMap<Long, T> aMap);
	public ConcurrentNavigableMap<Long, T> retrieve(Long aTime);
}
