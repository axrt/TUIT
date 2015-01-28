package taxonomy.node;

import taxonomy.Ranks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * A class that represents a node of a taxonomic tree with its taxid, parent {@link TaxonomicNode}, children {@link TaxonomicNode}s and scientific name
 */
public class TaxonomicNode {
    /**
     * The node's taxid
     */
    @SuppressWarnings("WeakerAccess")
    protected final int taxid;
    /**
     * The nodes {@link taxonomy.Ranks}
     */
    @SuppressWarnings("WeakerAccess")
    protected final Ranks rank;
    /**
     * A parent {@link TaxonomicNode}
     */
    @SuppressWarnings("WeakerAccess")
    protected TaxonomicNode parent;
    /**
     * A {@link List} of children {@link TaxonomicNode}s
     */
    @SuppressWarnings("WeakerAccess")
    protected final List<TaxonomicNode> children;
    /**
     * A set to prevent ambiguous children of the same reference
     */
    @SuppressWarnings("WeakerAccess")
    protected final Set<TaxonomicNode> childSet;
    /**
     * Shows if the node is root
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean isRoot;
    /**
     * The nodes scientific name (say, E.coli)
     */
    @SuppressWarnings("WeakerAccess")
    protected final String scientificName;

    /**
     * A protected constructor to use with static factories
     *
     * @param taxid          {@code int} taxid
     * @param rank           {@link Ranks} rank
     * @param scientificName {@link String} scientific name
     */
    @SuppressWarnings("WeakerAccess")
    protected TaxonomicNode(final int taxid, final Ranks rank, final String scientificName) {
        this.taxid = taxid;
        this.rank = rank;
        this.scientificName = scientificName;
        this.children = new ArrayList<TaxonomicNode>();
        this.childSet=new HashSet<>();
        this.isRoot = false;
    }

    /**
     * A getter for the taxid
     *
     * @return {@code int} taxid
     */
    public int getTaxid() {
        return this.taxid;
    }

    /**
     * A getter for the parent {@link TaxonomicNode}
     *
     * @return {@link TaxonomicNode} parent
     */
    public TaxonomicNode getParent() {
        return this.parent;
    }

    /**
     *
     * @return
     */
    public String getScientificName(){return this.scientificName;}
    /**
     * A getter for {@code boolean} isRoot
     *
     * @return {@code boolean} isRoot
     */
    public boolean isRoot() {
        return this.isRoot;
    }

    /**
     * A getter rot the {@link Ranks} rank
     *
     * @return {@link Ranks} rank
     */
    public Ranks getRank() {
        return this.rank;
    }

    /**
     * Adds a new child {@link TaxonomicNode} to the list of children
     *
     * @param child {@link TaxonomicNode}
     * @return {@code true} if success, otherwise - {@code false}
     */
    public boolean addChild(TaxonomicNode child) {
        if(this.childSet.add(child)){
            return this.children.add(child);
        }else{
            return false;
        }

    }

    public boolean justAddChild(TaxonomicNode child){
        return this.children.add(child);
    }

    /**
     * A getter for the {@link List} of {@link TaxonomicNode} children
     *
     * @return a {@link List} of {@link TaxonomicNode} children
     */
    public List<TaxonomicNode> getChildren() {
        return children;
    }

    /**
     * Sets the parent {@link TaxonomicNode}. If the parent has the same taxid as the taxid of this node,
     * then the node is considered root and sets the isRoot to {@code true}, otherwise remains {@code false}
     *
     * @param parent {@link TaxonomicNode}
     */
    public void setParent(TaxonomicNode parent) {
        this.parent = parent;
        this.isRoot = this.parent.getTaxid() == this.taxid;
    }

    /**
     * Checks whether the list of {@link TaxonomicNode} children contains a child node with the given taxid
     *
     * @param taxid {@code int} taxid
     * @return {@code true} if such a node exists, otherwise - {@code false}
     */
    public boolean isParentOf(int taxid) {
        for (TaxonomicNode taxonomicNode : this.children) {
            if (taxonomicNode.getTaxid() == taxid) {
                return true;
            } else if (taxonomicNode.isParentOf(taxid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this {@link TaxonomicNode} is an ancestor (not only an immediate child of) any node that
     * has a taxid of the given
     *
     * @param taxid {@code int} taxid of a hypothetical ancestor
     * @return returns {@code true} if the node is root or if the node has a {@link TaxonomicNode} with the given taxid
     *         in its lineage, {@code false} otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isChildOf(int taxid) {
        if (this.isRoot) {
            return this.taxid == taxid;
        } else {
            return this.parent.getTaxid() == taxid || this.parent.isChildOf(taxid);
        }
    }

    /**
     * Recursively appends all the parent scientific names and ranks until the root is reached
     *
     * @return A full taxonomic lineage from the root for this node separated by " -&lt; "
     */
    @Deprecated
    public String getFormattedLineage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (this.parent != null) {
            stringBuilder.append(parent.getFormattedLineage());
            stringBuilder.append(" -> ");
        }
        stringBuilder.append(this.getFormattedNameRank());
        return stringBuilder.toString();
    }

    /**
     * Returns a scientific name with ( taxonomic rank of the node)
     *
     * @return {@link String} representation of the name and taxonomic rank
     */
    @Deprecated
    @SuppressWarnings("WeakerAccess")
    public String getFormattedNameRank() {
        return this.scientificName + " {" + this.rank.getName() + "}";
    }

    //TODO document
    public boolean join(final TaxonomicNode otherNode){
        if(this.rank.equals(otherNode.rank)&&this.scientificName.equals(otherNode.scientificName)){
            final List<TaxonomicNode> combinedChildren=new ArrayList<>();
            final List<TaxonomicNode> toRemove=new ArrayList<>();
            combinedChildren.addAll(this.children);
            combinedChildren.addAll(otherNode.children);
            for(TaxonomicNode tn1:combinedChildren){
                for(TaxonomicNode tn2:combinedChildren){
                    if(!tn1.equals(tn2)&!toRemove.contains(tn1)&&tn1.join(tn2)){
                        toRemove.add(tn2);
                        break;
                    }
                }
            }
            combinedChildren.removeAll(toRemove);
            this.children.clear();
            this.children.addAll(combinedChildren);
            for(TaxonomicNode taxonomicNode:combinedChildren){
                taxonomicNode.setParent(this);
            }
            return true;
        }
        return false;
    }


    /**
     * A static factory to create a new instance of a {@link TaxonomicNode} from a given set of parameters
     *
     * @param taxid          {@code int} taxid
     * @param rank           {@link Ranks} rank
     * @param scientificName {@link String} scientific name
     * @return a new instance of {@link TaxonomicNode} from a given set of parameters
     */
    public static TaxonomicNode newDefaultInstance(final int taxid, final Ranks rank, final String scientificName) {
        return new TaxonomicNode(taxid, rank, scientificName);
    }
}