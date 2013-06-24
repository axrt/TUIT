package taxonomy.node;

import taxonomy.Ranks;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a node of a taxonomic tree with its taxid, paren {@link TaxonomicNode}, children {@link TaxonomicNode}s and scientific name
 */
public class TaxonomicNode {
    /**
     * The node's taxid
     */
    protected final int taxid;
    /**
     * The nodes {@link taxonomy.Ranks}
     */
    protected final Ranks rank;
    /**
     * A parent {@link TaxonomicNode}
     */
    protected TaxonomicNode parent;
    /**
     * A {@link List} of children {@link TaxonomicNode}s
     */
    protected final List<TaxonomicNode> children;
    /**
     * Shows if the node is root
     */
    protected boolean isRoot;
    /**
     * The nodes scientific name (say, E.coli)
     */
    protected final String scientificName;

    /**
     * A protected constructor to use with static factories
     *
     * @param taxid          {@code int} taxid
     * @param rank           {@link Ranks} rank
     * @param scientificName {@link String} scientific name
     */
    protected TaxonomicNode(final int taxid, final Ranks rank, final String scientificName) {
        this.taxid = taxid;
        this.rank = rank;
        this.scientificName = scientificName;
        this.children = new ArrayList<TaxonomicNode>();
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
     * A getter for {@link boolean} isRoot
     *
     * @return {@link boolean} isRoot
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
        if (this.parent.getTaxid() == this.taxid) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
    }

    //TODO: document
    public boolean isSiblingOf(int taxid) {
        if (this.taxid == taxid) {
            return true;
        } else {
            return false;
        }
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
    public boolean isChildOf(int taxid) {
        if (this.isRoot) {
            if (this.taxid == taxid) {
                return true;
            } else {
                return false;
            }
        } else {
            if (this.parent.getTaxid() == taxid) {
                return true;
            } else {
                return this.parent.isChildOf(taxid);
            }
        }
    }

    /**
     * Recursively appends all the parent scientific names and ranks until the root is reached
     *
     * @return A full taxinomic lineage from the root for this node separated by " -> "
     */
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
    public String getFormattedNameRank() {
        return this.scientificName + " (" + this.rank.getName() + ")";
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