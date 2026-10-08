package com.logai.deploy.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAnalysisServiceTests {

    @Test
    void sanitizeAiOutputRemovesCodeFencesAndTableFormatting() {

        String raw = String.join("\n",
                "```sql",
                "SELECT * FROM t;",
                "```",
                "| Error Type | Problem Summary |",
                "| --- | --- |",
                "| netty-pool-0 | Connection refused |"
        );

        String cleaned = AiAnalysisService.sanitizeAiOutput(raw);

        assertFalse(cleaned.contains("```"));
        assertFalse(cleaned.contains("| --- | --- |"));

        assertTrue(
                cleaned.contains("Error Type | Problem Summary")
        );

        assertTrue(
                cleaned.contains("netty-pool-0 | Connection refused")
        );
    }

    @Test
    void sanitizeAiOutputReturnsMessageForNull() {

        String result =
                AiAnalysisService.sanitizeAiOutput(null);

        assertEquals(
                "AI returned an empty response.",
                result
        );
    }

    @Test
    void sanitizeAiOutputReturnsMessageForBlankInput() {

        String result =
                AiAnalysisService.sanitizeAiOutput("   ");

        assertEquals(
                "AI returned an empty response.",
                result
        );
    }

    @Test
    void sanitizeAiOutputRemovesHtmlParagraphTags() {

        String raw =
                "<p>Problem Summary</p>\n<p>Connection refused</p>";

        String cleaned =
                AiAnalysisService.sanitizeAiOutput(raw);

        assertFalse(cleaned.contains("<p>"));
        assertFalse(cleaned.contains("</p>"));
        assertTrue(cleaned.contains("Problem Summary"));
        assertTrue(cleaned.contains("Connection refused"));
    }

    @Test
    void sanitizeAiOutputCollapsesExcessiveBlankLines() {

        String raw =
                "Error A\n\n\n\nError B";

        String cleaned =
                AiAnalysisService.sanitizeAiOutput(raw);

        assertEquals(
                "Error A\n\nError B",
                cleaned
        );
    }
}