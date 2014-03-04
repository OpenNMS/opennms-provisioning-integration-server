package org.opennms.pris.jdbc.source;

import java.io.OutputStream;

/**
 * Based on an article form David Van Couvering at http://davidvancouvering.blogspot.de/
 *
 */
public class DerbyUtil {

    public static final OutputStream DEV_NULL = new OutputStream() {
        @Override
        public void write(int b) {
        }
    };
}
