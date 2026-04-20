package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StackImplTest {
    private StackImpl<String> stack;

    @BeforeEach
    public void beforeEach(){
        this.stack = new StackImpl<>();
    }

    @Test
    public void testPush(){
        stack.push("pop me");
        Assertions.assertEquals("pop me", stack.pop());
        stack.push(null);
        Assertions.assertNull(stack.pop());
    }

    @Test
    public void testPop(){
        stack.push("test");
        Assertions.assertEquals("test", stack.pop());
        Assertions.assertNotEquals("test", stack.peek());
        Assertions.assertNull(stack.pop());
    }

    @Test
    public void testPeek(){
        stack.push("test");
        stack.push(null);
        Assertions.assertNull(stack.peek());
        stack.pop();
        Assertions.assertEquals("test", stack.peek());
        Assertions.assertEquals("test", stack.pop());
        Assertions.assertNull(stack.peek());
    }

    @Test
    public void testSize(){
        Assertions.assertEquals(0, stack.size());
        stack.push("a");
        stack.push("b");
        stack.push("c");
        Assertions.assertEquals(3, stack.size());
        stack.pop();
        Assertions.assertEquals(2, stack.size());
        stack.peek();
        Assertions.assertEquals(2, stack.size());
    }
}