package edu.yu.cs.com1320.project.impl;

import java.util.NoSuchElementException;
import edu.yu.cs.com1320.project.MinHeap;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E>{

    public MinHeapImpl(){
        elements = (E[]) new Comparable[10];
    }

    @Override
    /**
     * sets the structure of the heap to fit the heap contract
     * throws IAE if the element is not in the heap
     */
    public void reHeapify(E element){
        int spot = getArrayIndex(element);
        if (spot > 1 && isGreater(spot/2, spot)){
            upHeap(spot);
        } else {
            downHeap(spot);
        }
    }

    @Override
    /**
     * the index of this element in the array
     * throws IAE if the element isn't in the heap
     */
    protected int getArrayIndex(E element){
        for (int i = 1; i <= this.count; i++){
            if (elements[i].equals(element)){
                return i;
            }
        }
        throw new IllegalArgumentException("this element is not in the heap");
    }

    @Override
    protected void doubleArraySize(){
        E[] copy = (E[]) new Comparable[this.elements.length * 2];
        for (int i = 1; i <= this.count; i++){
            copy[i] = elements[i];
        }
        elements = copy;
    }
}