change-managed-values
=====================

This library offers a data structure which provides a history of changes made to a value. Undo and redo can be used to iterate forward or backward along the history. Random access to the history can be made by passing a high resolution time stamp to the getValue(Long time) method. The high resolution time stamp formula is ((time in millis that the class was initially loaded)+(System.nanoTime-(nanoTime at class loading)). Using undo does not change the value at the current time and is only a change in the externally visible value. To persist undo's you must undo or redo to the desired value, then update the value to be that value again. This way history is always preserved.
