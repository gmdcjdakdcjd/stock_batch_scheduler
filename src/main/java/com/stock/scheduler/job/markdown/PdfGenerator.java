/*
package com.stock.scheduler.job.markdown;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import java.io.*;
import java.util.List;

public class PdfGenerator {

    public static File generatePdfFromMarkdown(String markdown, String fileName) throws IOException {

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(markdown);
        String htmlContent = renderer.render(document);

        String html = """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <head>
                    <meta charset="UTF-8" />
                    <style>
                      body { font-family: 'Malgun Gothic', sans-serif; font-size: 12px; line-height: 1.5; }
                      table { border-collapse: collapse; width: 100%; border: 1px solid #999; }
                      th, td { border: 1px solid #999; padding: 4px; text-align: left; }
                      h1, h2, h3, h4 { color: #333; margin-top: 1rem; }
                      code { background-color: #f8f8f8; padding: 2px 4px; border-radius: 3px; }
                    </style>
                  </head>
                  <body>
                """ + htmlContent + "</body></html>";

        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");

        File pdfFile = new File(System.getProperty("java.io.tmpdir"), fileName + ".pdf");

        try (OutputStream os = new FileOutputStream(pdfFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(doc.html(), null);
            builder.toStream(os);

            // ✅ 한글 폰트 등록 (절대경로로 지정)
            builder.useFont(new File("C:/Windows/Fonts/malgun.ttf"), "Malgun Gothic");
            builder.useFont(new File("C:/Windows/Fonts/malgunbd.ttf"), "Malgun Gothic Bold");

            builder.run();
        }

        return pdfFile;
    }
}
*/
