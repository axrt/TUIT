package taxonomy;

import helper.Ranks;

import java.util.List;

/**
 * //Todo: document
 */
public class TaxonomicNode {

    protected final int taxid;
    protected final Ranks rank;
    protected final TaxonomicNode parent;
    protected final List<TaxonomicNode> children;
    protected final boolean isRoot;

    protected TaxonomicNode(final int taxid, final Ranks rank, final TaxonomicNode parent, final List<TaxonomicNode> children) {
        this.taxid = taxid;
        this.rank = rank;
        this.parent = parent;
        this.children = children;
        if (this.parent.getTaxid() == this.taxid) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
    }

    public int getTaxid() {
        return taxid;
    }

    public TaxonomicNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public boolean addChild(TaxonomicNode child) {
        return this.children.add(child);
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
}