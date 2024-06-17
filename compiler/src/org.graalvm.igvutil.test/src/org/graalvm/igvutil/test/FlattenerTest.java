package org.graalvm.igvutil.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.graalvm.igvutil.Flattener;
import org.junit.Assert;
import org.junit.Test;

import jdk.graal.compiler.graphio.parsing.DataBinaryWriter;
import jdk.graal.compiler.graphio.parsing.model.FolderElement;
import jdk.graal.compiler.graphio.parsing.model.GraphClassifier;
import jdk.graal.compiler.graphio.parsing.model.GraphDocument;
import jdk.graal.compiler.graphio.parsing.model.Group;
import jdk.graal.compiler.graphio.parsing.model.InputGraph;
import jdk.graal.compiler.graphio.parsing.model.KnownPropertyNames;
import jdk.graal.compiler.graphio.parsing.model.Properties;

public class FlattenerTest {
    public InputGraph createTestGraph(int dumpId, String name, String type) {
        InputGraph graph = new InputGraph(dumpId, name, new Object[]{});
        graph.setGraphType(type);
        Properties props = graph.writableProperties();
        props.setProperty("id", String.valueOf(dumpId));
        props.setProperty("graph", name);
        props.setProperty("graphType", type);
        graph.updateProperties(props);
        return graph;
    }

    public File createTestGraphFile() throws IOException {
        GraphDocument doc = new GraphDocument();
        Group group1 = new Group(doc);
        group1.writableProperties().setProperty(KnownPropertyNames.PROPNAME_NAME, "TestGroup1");
        group1.addElement(createTestGraph(0, "TestGraph", GraphClassifier.DEFAULT_TYPE));
        group1.addElement(createTestGraph(1, "TestGraph", GraphClassifier.DEFAULT_TYPE));

        Group group2 = new Group(doc);
        group2.writableProperties().setProperty(KnownPropertyNames.PROPNAME_NAME, "TestGroup2");
        group2.addElement(createTestGraph(0, "CallGraph", GraphClassifier.CALL_GRAPH));

        Group group3 = new Group(doc);
        group3.writableProperties().setProperty(KnownPropertyNames.PROPNAME_NAME, "TestGroup1");
        group3.addElement(createTestGraph(2, "TestGraph", GraphClassifier.DEFAULT_TYPE));
        group3.addElement(createTestGraph(3, "TestGraph", GraphClassifier.DEFAULT_TYPE));

        Group group4 = new Group(doc);
        group4.writableProperties().setProperty(KnownPropertyNames.PROPNAME_NAME, "TestGroup2");
        group4.addElement(createTestGraph(1, "CallGraph", GraphClassifier.CALL_GRAPH));

        doc.addElement(group1);
        doc.addElement(group2);
        doc.addElement(group3);
        doc.addElement(group4);

        File file = File.createTempFile("testGraph", ".bgv");
        file.deleteOnExit();

        DataBinaryWriter.export(file, doc, null, null);
        return file;
    }

    @Test
    public void testFlattenByGraph() throws IOException {
        File file = createTestGraphFile();
        Flattener flattener = new Flattener("graph");
        flattener.visitDump(Files.newInputStream(file.toPath()));

        GraphDocument outputDoc = flattener.getFlattenedDocument();
        Assert.assertEquals(2, outputDoc.getSize());
        for (FolderElement elem : outputDoc.getElements()) {
            Group g = (Group) elem;
            if (g.getName().equals("TestGraph")) {
                Assert.assertEquals(4, g.getGraphsCount());
            } else if (g.getName().equals("CallGraph")) {
                Assert.assertEquals(2, g.getGraphsCount());
            } else {
                Assert.fail("Unexpected group name: " + g.getName());
            }
        }
    }

    @Test
    public void testFlattenById() throws IOException {
        File file = createTestGraphFile();
        Flattener flattener = new Flattener("id");
        flattener.visitDump(Files.newInputStream(file.toPath()));

        GraphDocument outputDoc = flattener.getFlattenedDocument();
        Assert.assertEquals(4, outputDoc.getSize());
        for (FolderElement elem : outputDoc.getElements()) {
            Group g = (Group) elem;
            switch (g.getName()) {
                case "0":
                case "1":
                    Assert.assertEquals(2, g.getGraphsCount());
                    break;
                case "2":
                case "3":
                    Assert.assertEquals(1, g.getGraphsCount());
                    break;
                default:
                    Assert.fail("Unexpected group name: " + g.getName());
            }
        }
    }
}
