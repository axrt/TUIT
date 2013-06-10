package helper;

/**
 * //TODO: document
 *
 * A list of taxonomic ranks in order in which they appear in nature
 */
public enum Ranks {
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

    private final String name;

    private Ranks(String s) {
        name = s;
    }

    public String getName() {
        return name;
    }
}

