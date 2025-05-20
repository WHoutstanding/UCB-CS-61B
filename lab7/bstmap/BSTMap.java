package bstmap;

import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BSTMap<K extends Comparable<K> , V> implements Map61B<K, V> {
    private BSTNode<K> T;
    private int size;

    private class BSTNode<K extends Comparable<K>>  {
        private K key;
        private V value;
        private BSTNode<K> left;
        private BSTNode<K> right;

        public BSTNode() {}

        public BSTNode(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public BSTNode<K> find(BSTNode<K> T, K sk) {
            if (T == null) {
                return null;
            }

            if (sk.compareTo(T.key) < 0) {
                return find(T.left, sk);
            } else if (sk.compareTo(T.key) > 0) {
                return find(T.right, sk);
            }

            return T;
        }

        public BSTNode<K> insert(BSTNode<K> T, K ik, V value) {
            if (T == null) {
                return new BSTNode(ik, value);
            }

            if (ik.compareTo(T.key) < 0) {
                T.left = insert(T.left, ik, value);
            } else if (ik.compareTo(T.key) > 0) {
                T.right = insert(T.right, ik, value);
            } else if (ik.compareTo(T.key) == 0) {
                T.value = value;
            }

            return T;
        }

        public void printItem(BSTNode<K> T) {
            if (T == null) {
                return;
            }

            printItem(T.left);
            System.out.println(T.key + " " + T.value);
            printItem(T.right);
        }


    }

    public BSTMap() {
        T = new BSTNode<>();
        size = 0;
    }

    public BSTMap(K key, V value) {
        T = new BSTNode<>(key, value);
        size = 1;
    }

    /** Removes all of the mappings from this map. */
    public void clear() {

        T = new BSTNode<>();
        size = 0;
    }

    /* Returns true if this map contains a mapping for the specified key. */
    public boolean containsKey(K key) {
        if (size == 0) {
            return false;
        }

        return T.find(T, key).key != null;
    }

    /* Returns the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     */
    public V get(K key) {
        if (size == 0) {
            return null;
        }
        if (T.find(T, key) == null) {
            return null;
        }

        return T.find(T, key).value;
    }

    /* Returns the number of key-value mappings in this map. */
    public int size() {
//        throw new UnsupportedOperationException();
        return size;
    }

    /* Associates the specified value with the specified key in this map. */
    public void put(K key, V value) {
        if (size == 0) {
            T.key = key;
            T.value = value;
        } else{
            T = T.insert(T, key, value);
        }
        size += 1;
    }

    public void printInOrder() {
        T.printItem(T);
    }


    /* Returns a Set view of the keys contained in this map. Not required for Lab 7.
     * If you don't implement this, throw an UnsupportedOperationException. */
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /* Removes the mapping for the specified key from this map if present.
     * Not required for Lab 7. If you don't implement this, throw an
     * UnsupportedOperationException. */
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    /* Removes the entry for the specified key only if it is currently mapped to
     * the specified value. Not required for Lab 7. If you don't implement this,
     * throw an UnsupportedOperationException.*/
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<K> iterator() {
        return null;
    }

    public static void main(String[] args) {
        BSTMap<String, Integer> b = new BSTMap<String, Integer>();
    }
}
