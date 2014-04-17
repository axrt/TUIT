package db.ram.row;

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
 * A container for GI - taxId pair
 */
public class GiTaxIdRow extends RamRow<Integer,Integer> {

    /**
     * A protected constructor, which takes both arguments from a pair
     * @param integer {@link java.lang.Integer} GI
     * @param integer2  {@link java.lang.Integer} taxId
     */
    protected GiTaxIdRow(Integer integer, Integer integer2) {
        super(integer, integer2);
    }

    /**
     * Static factory, that creates a new instance of the row.
     * @param gi  {@link java.lang.Integer} GI
     * @param taxid {@link java.lang.Integer} taxId
     * @return {@link db.ram.row.RamRow} from parameters
     */
    public static RamRow<Integer,Integer> newInstance(Integer gi, Integer taxid){
        return new GiTaxIdRow(gi,taxid);
    }
}
