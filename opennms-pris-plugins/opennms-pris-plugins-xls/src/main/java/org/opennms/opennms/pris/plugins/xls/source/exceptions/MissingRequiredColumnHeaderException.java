package org.opennms.opennms.pris.plugins.xls.source.exceptions;

public class MissingRequiredColumnHeaderException extends Exception {

    public MissingRequiredColumnHeaderException(String columnName) {
        this(columnName, null);
    }

    public MissingRequiredColumnHeaderException(String columnName, Throwable cause) {
        super("Required Column-Header is missing: " + columnName, cause);
    }
}