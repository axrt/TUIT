package taxonomy;

import helper.Ranks;

import java.util.ArrayList;
import java.util.List;

/**
 * //Todo: document
 */
public class TaxonomicNode {

    protected final int taxid;
    protected final Ranks rank;
    protected TaxonomicNode parent;
    protected final List<TaxonomicNode> children;
    protected boolean isRoot;
    protected final String scientificName;

    protected TaxonomicNode(final int taxid, final Ranks rank, final String scientificName) {
        this.taxid = taxid;
        this.rank=rank;
        this.scientificName=scientificName;
        this.children = new ArrayList<TaxonomicNode>();
        this.isRoot=false;
    }

    public int getTaxid() {
        return this.taxid;
    }

    public TaxonomicNode getParent() {
        return this.parent;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public Ranks getRank() {
        return this.rank;
    }

    public boolean addChild(TaxonomicNode child) {
        return this.children.add(child);
    }

    public List<TaxonomicNode> getChildren() {
        return children;
    }

    public void setParent(TaxonomicNode parent) {
        this.parent = parent;
        if (this.parent.getTaxid() == this.taxid) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
    }

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
     *
     * @param taxid
     * @param rank
     * @param scientificName
     * @return
     */
    public static TaxonomicNode newDefaultInstance(final int taxid, final Ranks rank, final String scientificName){
        return new TaxonomicNode(taxid,rank,scientificName);
    }
}