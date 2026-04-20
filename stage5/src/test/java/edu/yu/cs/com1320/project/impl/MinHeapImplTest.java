package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class MinHeapImplTest {
    private MinHeapImpl<Document> heap;
    URI uri1 = URI.create("https://fakeurl1.com");
    URI uri2 = URI.create("https://fakeurl2.com");
    URI uri3 = URI.create("https://fakeurl3.com");
    DocumentImpl doc = new DocumentImpl(uri1, "text");
    DocumentImpl doc2 = new DocumentImpl(uri2, "doc");
    DocumentImpl doc3 = new DocumentImpl(uri3, "third");

    @BeforeEach
    public void beforeEach(){
        this.heap = new MinHeapImpl<>();
        doc.setLastUseTime(4);
        doc2.setLastUseTime(5);
        doc3.setLastUseTime(6);
    }

    @Test
    //reheapify throws IAE if the element is not in the heap
    public void testReHeapifyThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            heap.reHeapify(doc);
        });
        heap.insert(doc);
        heap.insert(doc2);
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            heap.reHeapify(doc3);
        });
    }

    @Test
    //reheapify moves a doc with a low time to the top
    public void testReHeapifyMoveUp(){
        heap.insert(doc);
        heap.insert(doc2);
        heap.insert(doc3);
        doc3.setLastUseTime(1);
        heap.reHeapify(doc3);
        Assertions.assertEquals(doc3, heap.remove());
    }

    @Test
    //reheapify moves a doc with a high time to the bottom
    public void testReHeapifyMoveDown(){
        heap.insert(doc);
        heap.insert(doc2);
        heap.insert(doc3);
        doc.setLastUseTime(10);
        heap.reHeapify(doc);
        heap.remove();
        heap.remove();
        Assertions.assertEquals(doc, heap.remove());
    }

    @Test
    //reheapify doesn't move a doc when it isn't supposed to
    public void testReHeapifyNoMove() {
        heap.insert(doc);
        heap.insert(doc2);
        heap.insert(doc3);
        doc.setLastUseTime(1);
        heap.reHeapify(doc);
        doc2.setLastUseTime(3);
        heap.reHeapify(doc2);
        heap.remove();
        Assertions.assertEquals(doc2, heap.remove());
    }
}