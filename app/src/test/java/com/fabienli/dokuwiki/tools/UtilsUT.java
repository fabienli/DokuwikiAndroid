package com.fabienli.dokuwiki.tools;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UtilsUT {


    /**
     * Test sync URL with sync part in the middle
     */
    @Test
    public void test_CopyStream(){
        String inputString = "test content";
        InputStream is = new ByteArrayInputStream(inputString.getBytes());
        OutputStream os = new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                this.string.append((char) b );
            }

            //Netbeans IDE automatically overrides this toString()
            public String toString() {
                return this.string.toString();
            }
        };
        Utils.CopyStream(is, os);
        assert(os.toString().compareTo(inputString) == 0);
    }
}
