package org.opennms.pris.xls.source;

class InvalidInterfaceException extends Exception {

    public InvalidInterfaceException(String string, IllegalArgumentException ex) {
        super(string, ex);
    }
}
