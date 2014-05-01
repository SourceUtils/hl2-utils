package com.timepath.hl2;

import com.timepath.steam.io.VDF2;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class VDFDiffTest {

    private static final Logger LOG = Logger.getLogger(VDFDiffTest.class.getName());

    public static void main(String... args) {

        /**
         * "VDF" {
         * "Modified" {
         * "Same"	"yes"
         * "Similar"	"one"
         * "Removed"	"yes"
         * }
         * "Removed" {}
         * "Same" {}
         * }
         */
        final VDF2 n1 = new VDF2("A");
        
         /**
         * "VDF" {
         * "Modified" {
         * "Same"	"yes"
         * "Similar"	"two"
         * "Added"	"yes"
         * }
         * "New" {}
         * "Same" {}
         * }
         */
        final VDF2 n2 = new VDF2("B");

        VDF2 tmp = new VDF2("Modified");
        tmp.addAllProperties(new VDF2.VDFProperty("Same", "yes"),
                             new VDF2.VDFProperty("Similar", "one"),
                             new VDF2.VDFProperty("Removed", "yes"));
        n1.addNode(tmp);
        n1.addNode(new VDF2("Removed"));
        n1.addNode(new VDF2("Same"));

        VDF2 tmp2 = new VDF2("Modified");
        tmp2.addAllProperties(new VDF2.VDFProperty("Same", "yes"),
                              new VDF2.VDFProperty("Similar", "two"),
                              new VDF2.VDFProperty("Added", "yes"));
        n2.addNode(tmp2);
        n2.addNode(new VDF2("New"));
        n2.addNode(new VDF2("Same"));

        n1.rdiff2(n2);
        
//        Node.debugDiff(n1.rdiff(n2));
    }

}
