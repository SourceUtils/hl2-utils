package com.timepath.hl2;

import com.timepath.steam.io.VDFNode;

import java.util.logging.Logger;

/**
 * @author TimePath
 */
class VDFDiffTest {

    private static final Logger LOG = Logger.getLogger(VDFDiffTest.class.getName());

    private VDFDiffTest() {}

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
        VDFNode n1 = new VDFNode("A");
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
        VDFNode n2 = new VDFNode("B");
        VDFNode tmp = new VDFNode("Modified");
        tmp.addAllProperties(new VDFNode.VDFProperty("Same", "yes"),
                             new VDFNode.VDFProperty("Similar", "one"),
                             new VDFNode.VDFProperty("Removed", "yes"));
        n1.addNode(tmp);
        n1.addNode(new VDFNode("Removed"));
        n1.addNode(new VDFNode("Same"));
        VDFNode tmp2 = new VDFNode("Modified");
        tmp2.addAllProperties(new VDFNode.VDFProperty("Same", "yes"),
                              new VDFNode.VDFProperty("Similar", "two"),
                              new VDFNode.VDFProperty("Added", "yes"));
        n2.addNode(tmp2);
        n2.addNode(new VDFNode("New"));
        n2.addNode(new VDFNode("Same"));
        n1.rdiff2(n2);
        //        Node.debugDiff(n1.rdiff(n2));
    }
}
