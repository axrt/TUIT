package helper;

/**
 * A list of taxonomic ranks in order in which they appear in the NCBI taxonomy
 */
public enum Ranks {

    root_of_life("root of life"),
    no_rank("no rank"),
    superkingdom("superkingdom"),
    kingdom("kingdom"),
    subkingdom("subkingdom"),
    superphylum("superphylum"),
    phylum("phylum"),
    subphylum("subphylum"),
    superclass("superclass"),
    c_lass("class"),
    subclass("subclass"),
    infraclass("infraclass"),
    superorder("superorder"),
    order("order"),
    suborder("suborder"),
    infraorder("infraorder"),
    parvorder("parvorder"),
    superfamily("superfamily"),
    family("family"),
    subfamily("subfamily"),
    tribe("tribe"),
    subtribe("subtribe"),
    genus("genus"),
    subgenus("subgenus"),
    species_group("species group"),
    species_subgroup("species subgroup"),
    species("species"),
    subspecies("subspecies"),
    varietas("varietas"),
    forma("forma");

    public static String LIST_RANKS;
    static {
        StringBuilder stringBuilder=new StringBuilder();
        for(Ranks r:Ranks.values()){
            stringBuilder.append(r.getName());
            stringBuilder.append("\n");
        }
        Ranks.LIST_RANKS=stringBuilder.toString();
    }
    /**
     * A {@link String} representation of the name
     */
    private final String name;

    /**
     * Private constructor
     * @param s {@link String} of the name for the given rank
     */
    private Ranks(String s) {
        name = s;
    }

    /**
     * A getter for the name
     * @return {@link String} name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a previous {@link Ranks} for a given {@link Ranks}
     * @param rank {@link Ranks}
     * @return a previous {@link Ranks} for a given {@link Ranks} if such exists, otherwise, if the
     * {@link Ranks.root_of_life} given - the {@link Ranks.root_of_life} will be returned
     */
    public static Ranks previous (Ranks rank){
       if(rank==Ranks.root_of_life){
          return root_of_life;
       }else{
           return Ranks.values()[rank.ordinal()-1];
       }
    }
}

