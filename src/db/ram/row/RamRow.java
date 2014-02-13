package db.ram.row;

import java.io.Serializable;

/**
 * Created by alext on 2/12/14.
 */
//TODO: document
public abstract class RamRow<K,V> implements Serializable {

    protected final K k;
    protected final V v;

    protected RamRow(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getK() {
        return k;
    }

    public V getV() {
        return v;
    }
}
