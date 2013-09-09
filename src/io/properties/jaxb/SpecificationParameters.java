//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.17 at 12:20:55 PM MSK 
//


package io.properties.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
 * 
 */
@SuppressWarnings("WeakerAccess")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "cutoffSet"
})
@XmlRootElement(name = "SpecificationParameters")
public class SpecificationParameters {

    @XmlElement(name = "CutoffSet", required = true)
    protected CutoffSet cutoffSet;

    /**
     * Gets the value of the cutoffSet property.
     * 
     * @return
     *     possible object is
     *     {@link CutoffSet }
     *     
     */
    public CutoffSet getCutoffSet() {
        return cutoffSet;
    }

    /**
     * Sets the value of the cutoffSet property.
     * 
     * @param value
     *     allowed object is
     *     {@link CutoffSet }
     *     
     */
    public void setCutoffSet(CutoffSet value) {
        this.cutoffSet = value;
    }

}
