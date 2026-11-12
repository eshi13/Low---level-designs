package com.embedcache.eviction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Least-Recently-Used eviction: doubly linked list + map for O(1) move-to-front.
 * Head = most recently used; tail = least recently used (eviction candidate).
 *
 * @param <K> key type
 */
public final class LruEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, Node<K>> nodes = new HashMap<>();
    private Node<K> head;
    private Node<K> tail;

    @Override
    public void recordAccess(K key) {
        Objects.requireNonNull(key, "key");
        Node<K> node = nodes.get(key);
        if (node == null) {
            throw new IllegalArgumentException("Unknown key for access: " + key);
        }
        moveToHead(node);
    }

    @Override
    public void recordInsert(K key) {
        Objects.requireNonNull(key, "key");
        if (nodes.containsKey(key)) {
            throw new IllegalArgumentException("Key already present: " + key);
        }
        Node<K> node = new Node<>(key);
        nodes.put(key, node);
        addToHead(node);
    }

    @Override
    public void recordRemoval(K key) {
        Objects.requireNonNull(key, "key");
        Node<K> node = nodes.remove(key);
        if (node != null) {
            unlink(node);
        }
    }

    @Override
    public K evictKey() {
        if (tail == null) {
            return null;
        }
        return tail.key;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public void clear() {
        nodes.clear();
        head = null;
        tail = null;
    }

    private void addToHead(Node<K> node) {
        node.prev = null;
        node.next = head;
        if (head != null) {
            head.prev = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
    }

    private void moveToHead(Node<K> node) {
        if (node == head) {
            return;
        }
        unlink(node);
        addToHead(node);
    }

    private void unlink(Node<K> node) {
        Node<K> prev = node.prev;
        Node<K> next = node.next;
        if (prev != null) {
            prev.next = next;
        } else {
            head = next;
        }
        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev;
        }
        node.prev = null;
        node.next = null;
    }

    private static final class Node<K> {
        final K key;
        Node<K> prev;
        Node<K> next;

        Node(K key) {
            this.key = key;
        }
    }
}
