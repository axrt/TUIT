package db.tables;
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
 * Provides common lookup names for the NCBI schema within the database
 */
public class LookupNames {

    /**
     * Constructor grants non-insatiability
     */
    private LookupNames() {
        throw new AssertionError();
    }

    /**
     * Represents the tables namespace
     */
    //Database names
    public static class dbs {
        public static class NCBI {
            public static final String name = "NCBI";

            public static class ranks {
                public static final String name = "ranks";

                public enum columns {
                    id_ranks,
                    rank
                }
            }
            public static class gi_taxid {
                public static final String name = "GI_TAXID";
                public enum columns {
                    gi,
                    taxid
                }
            }
            public static class names {
                public static final String name = "names";
                public enum columns {
                    taxid,
                    name
                }
            }
            public static class nodes {
                public static final String name = "nodes";
                public enum columns {
                    id_nodes,
                    taxid,
                    parent_taxid,
                    id_ranks
                }
            }
            public enum views{
                taxon_by_gi("taxon_by_gi"),
                f_level_children_by_parent("f_level_children_by_parent"),
                rank_by_taxid("rank_by_taxid");

                private final String name;
                private views(String name){
                     this.name=name;
                }
                public String getName(){
                    return this.name;
                }
            }
        }
    }

}
