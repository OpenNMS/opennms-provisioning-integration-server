package org.opennms.pris.xls.source;

public class MissingRequiredColumnHeaderException extends Exception {

    public MissingRequiredColumnHeaderException(String columnName) {
        this(columnName, null);
    }

    public MissingRequiredColumnHeaderException(String columnName, Throwable cause) {
        super("Required Column-Header is missing: " + columnName, cause);
    }
}