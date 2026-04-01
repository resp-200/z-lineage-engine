package demo.lineage;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图谱产物校验测试。
 */
public class AppTest {

    @Test
    public void shouldGenerateLineageArtifacts() throws IOException {
        Path jsonPath = Paths.get("target", "lineage", "graph.json");
        Path mermaidPath = Paths.get("target", "lineage", "graph.mmd");

        Assert.assertTrue("graph.json 不存在", Files.exists(jsonPath));
        Assert.assertTrue("graph.mmd 不存在", Files.exists(mermaidPath));

        String json = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
        String mermaid = new String(Files.readAllBytes(mermaidPath), StandardCharsets.UTF_8);

        Assert.assertTrue(json.contains("\"type\":\"PRECEDES\""));
        Assert.assertTrue(json.contains("\"type\":\"MUTEX\""));
        Assert.assertTrue(mermaid.contains("flowchart LR"));
        Assert.assertTrue(mermaid.contains("MUTEX"));
    }
}
