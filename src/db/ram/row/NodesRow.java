package db.ram.row;

import taxonomy.Ranks;

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
 * A container for taxId - parent node taxId pair - taxonomic rank
 */
public class NodesRow extends RamRow<Integer,Integer> {

    /**
     * {@link taxonomy.Ranks} that corresponds to the taxId
     */
    protected final Ranks rank;

    /**
     * A protected constructor to create a row from three parameters
     * @param taxid  {@link java.lang.Integer} taxId
     * @param partentTaxid {@link java.lang.Integer} taxId of a parent
     * @param rank {@link taxonomy.Ranks} corresponding taxonomic rank if the given taxId
     */
    protected NodesRow(Integer taxid, Integer partentTaxid, Ranks rank) {
        super(taxid, partentTaxid);
        this.rank = rank;
    }

    /**
     * Getter for the {@link taxonomy.Ranks} taxonomic rank
     * @return {@link taxonomy.Ranks} taxonomic rank
     */
    public Ranks getRank() {
        return rank;
    }

    /**
     * Static factory to create a new instance of the row
     * @param taxid  {@link java.lang.Integer} taxId
     * @param partentTaxid {@link java.lang.Integer} taxId of a parent
     * @param rank {@link taxonomy.Ranks} corresponding taxonomic rank if the given taxId
     * @return a new instance of {@link db.ram.row.NodesRow}
     */
    public static NodesRow newInstance(Integer taxid, Integer partentTaxid, Ranks rank){
        return new NodesRow(taxid,partentTaxid,rank);
    }
}
