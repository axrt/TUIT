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
 * A container for taxId - Scientific name pair
 */
public class NamesRow extends RamRow<Integer,String> {
    /**
     * A protected constructor to create a row from two parameters
     * @param integer {@link java.lang.Integer} taxID
     * @param s {@link java.lang.String} scientific name
     */
    protected NamesRow(Integer integer, String s) {
        super(integer, s);
    }

    /**
     * A static factory to get a row
     * @param taxid {@link java.lang.Integer} taxID
     * @param scientificName {@link java.lang.String} scientific name
     * @return new {@link db.ram.row.RamRow} from parameters
     */
    public static RamRow<Integer,String> newInstance(Integer taxid, String scientificName){
        return new NamesRow(taxid,scientificName);
    }
}
