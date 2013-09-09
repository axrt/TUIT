package exception;

import format.BadFormatException;
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
 * A type of exception that should be thrown any time that an error or a badly formatted property appears within the
 * properties configuration file
 */
public class TUITPropertyBadFormatException extends BadFormatException {
    /**
     * Call tot the super constructor
     */
    public TUITPropertyBadFormatException() {
        super();
    }
    /**
     * Call tot the super constructor with message
     */
    public TUITPropertyBadFormatException(String message) {
        super(message);
    }
}
