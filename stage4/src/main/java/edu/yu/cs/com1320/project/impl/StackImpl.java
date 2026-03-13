package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    class Element<T>{
        private T self;
        private Element next;
        Element(T me){
            this.self = me;
            this.next = null;
        }
    }

    private Element<T> head;
    private int size;

    public StackImpl(){
        this.head = null;
        this.size = 0;
    }

    /**
     * @param element object to add to the Stack
     */
    public void push(T element){
        Element<T> newTop = new Element(element);
        newTop.next = this.head;
        this.head = newTop;
        this.size++;
    }

    /**
     * removes and returns element at the top of the stack
     * @return element at the top of the stack, null if the stack is empty
     */
    public T pop(){
        if (this.size == 0){
            return null;
        }
        Element<T> oldTop = this.head;
        this.head = this.head.next;
        this.size--;
        return oldTop.self;
    }

    /**
     *
     * @return the element at the top of the stack without removing it
     */
    public T peek(){
        if (this.size == 0){
            return null;
        }
        Element<T> top = this.head;
        return top.self;
    }

    /**
     *
     * @return how many elements are currently in the stack
     */
    public int size(){
        return this.size;
    }
}