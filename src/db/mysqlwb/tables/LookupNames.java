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
            public static String name = "NCBI";

            public static class ranks {
                public static String name = "ranks";

                public enum columns {
                    id_ranks,
                    rank;
                }
            }
        }
    }
}
