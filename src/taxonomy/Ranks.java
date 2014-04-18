package taxonomy;
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
     * A helper method to convert a {@link String} representations of
     * the taxonomic ranks a {@link taxonomy.Ranks} enum form.
     * @param strValue {@link String} that gets to be converted.
     * @return {@link taxonomy.Ranks} of the {@link String} representation
     */
    public static Ranks convertValue(final String strValue){
        switch (strValue){
            case "class":return c_lass;
            case "no rank":return no_rank;
            case "species group":return species_group;
            case "species subgroup":return species_subgroup;
            case "root of life":return root_of_life;
            default:return Ranks.valueOf(strValue);
        }
    }
    /**
     * Returns a previous {@link Ranks} for a given {@link Ranks}
     * @param rank {@link Ranks}
     * @return a previous {@link Ranks} for a given {@link Ranks} if such exists, otherwise, if the
     * {@link Ranks}.root_of_life is given - the {@link Ranks}.root_of_life will be returned
     */
    public static Ranks previous (Ranks rank){
       if(rank==Ranks.root_of_life){
          return root_of_life;
       }else{
           return Ranks.values()[rank.ordinal()-1];
       }
    }
}

