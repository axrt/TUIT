package db.ram.row;

import java.io.Serializable;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * An abstracton of a container of a simple database row-like entity, that keeps a key-value pair. Example: GI-taxId pair.
 * Is immutable.
 */
public abstract class RamRow<K,V> implements Serializable {
    /**
     * Key
     */
    protected final K k;
    /**
     * Value
     */
    protected final V v;

    /**
     * Protected constructor from parameters.
     * @param k key
     * @param v value
     */
    protected RamRow(K k, V v) {
        this.k = k;
        this.v = v;
    }

    /**
     * Getter for the key.
     * @return key
     */
    public K getK() {
        return k;
    }

    /**
     * Getterfor the value
     * @return value
     */
    public V getV() {
        return v;
    }
}
