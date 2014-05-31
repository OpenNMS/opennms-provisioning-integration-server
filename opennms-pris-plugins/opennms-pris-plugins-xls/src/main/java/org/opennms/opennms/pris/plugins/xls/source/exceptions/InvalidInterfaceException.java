package org.opennms.opennms.pris.plugins.xls.source.exceptions;

public class InvalidInterfaceException extends Exception {

    public InvalidInterfaceException(String string, IllegalArgumentException ex) {
        super(string, ex);
    }
}
