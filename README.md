# Document Store and Search Engine

A Java document store and search engine, built from the ground up over a semester. It stores text and binary documents, supports keyword, prefix, and metadata search ranked by relevance, offers multi-level undo, and manages memory by moving the least recently used documents to disk and loading them back automatically when they're needed.

Every core data structure is implemented from scratch, without using Java's built-in collections.

Built as the semester project for Data Structures (COM 1320) at Yeshiva University, Spring 2026.

## Features

- **Document storage:** Store and retrieve text or binary documents by URI, with key-value metadata on each document.
- **Search:** Find documents by keyword, by word prefix, by metadata, or by a combination. Results are ranked by how often the search term appears in each document.
- **Bulk delete:** Delete every document that matches a keyword, prefix, or metadata query in a single call.
- **Undo:** Undo the most recent action, or the most recent action on a specific document, without disturbing later changes to other documents. Bulk operations can be undone as a whole or one document at a time.
- **Memory limits:** Set a maximum number of documents or bytes held in memory. When a limit is reached, the least recently used documents are written to disk as JSON and reloaded automatically the next time they're accessed.

## Data structures implemented

| Structure | Role in the system |
|---|---|
| **Hash table** | Generic, separate chaining, with array doubling and rehashing. Primary document storage in stages 2–5. |
| **Stack** | Command stack that powers undo. |
| **Trie** | Indexes every word in every document for keyword and prefix search, removing empty branches on deletion. |
| **Min-heap** | Orders documents by last-use time so the least recently used document can be found quickly. |
| **B-tree** | Primary document storage in stage 6, where each entry points either to a document in memory or to its location on disk. |

## Design highlights

- **Undo with the Command pattern:** Every change pushes a command onto the stack, and each command's undo logic is a Java lambda. Operations that affect many documents are grouped into a single command set, so they can be undone together or one document at a time.
- **Two-tier storage:** The B-tree stays in memory while documents move between memory and disk. Serialization is handled by a separate persistence manager using Gson, which saves each document's text or binary content, metadata, and word-count index.
- **Single owner for documents:** In the final stage, only the B-tree holds direct references to documents. The trie, heap, and undo stack refer to documents by URI, so a document moved to disk is never accidentally kept alive in memory.
- **Clean code constraints:** No static methods besides `main`, no method longer than 30 lines, and no libraries beyond the JDK and Gson.

## Project structure

The project was built in six stages. Each one is a complete, independently buildable Maven project that adds a feature to the one before it:

| Stage | What it adds |
|---|---|
| stage1 | In-memory document store with metadata |
| stage2 | Custom hash table replaces `java.util.HashMap` |
| stage3 | Undo with a command stack; dynamic resizing of the hash table |
| stage4 | Keyword, prefix, and metadata search with a trie |
| stage5 | Memory limits with least-recently-used eviction using a min-heap |
| stage6 | B-tree storage with disk persistence for evicted documents |

stage6 is the finished system.

## Running the tests

Requirements: Java 17 and Maven.

```
cd stage6
mvn test
```

## Tech stack

Java 17 · Maven · JUnit · Gson
