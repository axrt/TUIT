//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.04.17 at 06:11:22 PM EDT 
//


package io.properties.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pIdentCutoff",
    "queryCoverageCutoff",
    "alpha"
})
@XmlRootElement(name = "CutoffSet")
public class CutoffSet {

    @XmlAttribute(name = "rank", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String rank;
    @XmlElement(required = true)
    protected PIdentCutoff pIdentCutoff;
    @XmlElement(name = "QueryCoverageCutoff", required = true)
    protected QueryCoverageCutoff queryCoverageCutoff;
    @XmlElement(name = "Alpha", required = true)
    protected Alpha alpha;

    /**
     * Gets the value of the rank property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRank() {
        return rank;
    }

    /**
     * Sets the value of the rank property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRank(String value) {
        this.rank = value;
    }

    /**
     * Gets the value of the pIdentCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link PIdentCutoff }
     *     
     */
    public PIdentCutoff getPIdentCutoff() {
        return pIdentCutoff;
    }

    /**
     * Sets the value of the pIdentCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link PIdentCutoff }
     *     
     */
    public void setPIdentCutoff(PIdentCutoff value) {
        this.pIdentCutoff = value;
    }

    /**
     * Gets the value of the queryCoverageCutoff property.
     * 
     * @return
     *     possible object is
     *     {@link QueryCoverageCutoff }
     *     
     */
    public QueryCoverageCutoff getQueryCoverageCutoff() {
        return queryCoverageCutoff;
    }

    /**
     * Sets the value of the queryCoverageCutoff property.
     * 
     * @param value
     *     allowed object is
     *     {@link QueryCoverageCutoff }
     *     
     */
    public void setQueryCoverageCutoff(QueryCoverageCutoff value) {
        this.queryCoverageCutoff = value;
    }

    /**
     * Gets the value of the alpha property.
     * 
     * @return
     *     possible object is
     *     {@link Alpha }
     *     
     */
    public Alpha getAlpha() {
        return alpha;
    }

    /**
     * Sets the value of the alpha property.
     * 
     * @param value
     *     allowed object is
     *     {@link Alpha }
     *     
     */
    public void setAlpha(Alpha value) {
        this.alpha = value;
    }

}
