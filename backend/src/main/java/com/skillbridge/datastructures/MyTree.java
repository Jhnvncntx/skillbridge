package com.skillbridge.datastructures;

/**
 * General (n-ary) tree. Each node keeps its children in a MyLinkedList.
 * Used for the College -> Program -> Major hierarchy.
 * Two traversals are provided:
 *   preorder()   - depth-first, iterative, using MyStack
 *   levelOrder() - breadth-first, using MyQueue
 */
public class MyTree<T> {
    public static class Node<T> {
        public final T data;
        public final MyLinkedList<Node<T>> children = new MyLinkedList<>();

        Node(T data) { this.data = data; }

        public Node<T> addChild(T childData) {
            Node<T> child = new Node<>(childData);
            children.add(child);
            return child;
        }
    }

    private final Node<T> root;

    public MyTree(T rootData) { root = new Node<>(rootData); }

    public Node<T> getRoot() { return root; }

    /** Depth-first (parent before children, left to right). */
    public MyLinkedList<T> preorder() {
        MyLinkedList<T> result = new MyLinkedList<>();
        MyStack<Node<T>> stack = new MyStack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Node<T> node = stack.pop();
            result.add(node.data);
            // push children in reverse so the first child is popped first
            Object[] kids = new Object[node.children.size()];
            int i = 0;
            for (Node<T> c : node.children) kids[i++] = c;
            for (int k = kids.length - 1; k >= 0; k--) {
                @SuppressWarnings("unchecked") Node<T> child = (Node<T>) kids[k];
                stack.push(child);
            }
        }
        return result;
    }

    /** Breadth-first (level by level). */
    public MyLinkedList<T> levelOrder() {
        MyLinkedList<T> result = new MyLinkedList<>();
        MyQueue<Node<T>> queue = new MyQueue<>();
        queue.enqueue(root);
        while (!queue.isEmpty()) {
            Node<T> node = queue.dequeue();
            result.add(node.data);
            for (Node<T> c : node.children) queue.enqueue(c);
        }
        return result;
    }

    public int size() { return preorder().size(); }
}
