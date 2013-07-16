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

import java.util.Stack;


public class DomainObject {
	private ChangeManagedValue<Integer> i;
	private ChangeManagedValue<String> x;
	private ChangeManagedValue<Boolean> y;
	private Stack<ChangeManagedValue> undoStack = new Stack<ChangeManagedValue>();
	private Stack<ChangeManagedValue> redoStack = new Stack<ChangeManagedValue>();
	
	public Integer getI() {
		return i.getValue();
	}
	public void setI(Integer i) {
		this.i.update(i);
		undoStack.push(this.i);
	}
	
	public String getX() {
		return x.getValue();
	}
	public void setX(String x) {
		this.x.update(x);
		undoStack.push(this.x);
	}
	
	public Boolean getY() {
		return y.getValue();
	}
	public void setY(Boolean y) {
		this.y.update(y);
		undoStack.push(this.y);
	}
	
	public void undo(){
		ChangeManagedValue lastChange = undoStack.pop();
		redoStack.push(lastChange);
		lastChange.undo();
	}
	public void redo(){
		ChangeManagedValue lastUndo = redoStack.pop();
		undoStack.push(lastUndo);
		lastUndo.undo();
	}
}
