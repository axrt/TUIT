package db.mysqlwb.tables;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 5/31/13
 * Time: 12:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class LookupNames {

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
                    rank;
                }
            }
            public static class gi_taxid {
                public static final String name = "GI_TAXID";
                public enum columns {
                    gi,
                    taxid;
                }
            }
            public static class names {
                public static final String name = "names";
                public enum columns {
                    taxid,
                    name;
                }
            }
            public static class nodes {
                public static final String name = "nodes";
                public enum columns {
                    id_nodes,
                    taxid,
                    parent_taxid,
                    id_ranks;
                }
            }
        }
    }
}
