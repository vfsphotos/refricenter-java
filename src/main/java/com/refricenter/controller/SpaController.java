package com.refricenter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;

/**
 * Serves the SPA index.html by resolving Django-style template tags at startup.
 * Partials are inlined once and cached for subsequent requests.
 */
@RestController
public class SpaController {

    @Value("${app.templates-root:./templates}")
    private String templatesRoot;

    private final AtomicReference<String> cachedIndex = new AtomicReference<>();
    private final AtomicReference<String> cachedManual = new AtomicReference<>();

    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() throws IOException {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(getRendered("index.html", cachedIndex));
    }

    @GetMapping(value = {"/manual", "/manual.html", "/manual/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> manual() throws IOException {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(getRendered("manual.html", cachedManual));
    }

    private String getRendered(String name, AtomicReference<String> cache) throws IOException {
        String html = cache.get();
        if (html == null) {
            html = render(name);
            cache.set(html);
        }
        return html;
    }

    private String render(String templateName) throws IOException {
        Path templatePath = Paths.get(templatesRoot, templateName);
        String content = Files.readString(templatePath);

        content = content.replaceAll("\\{%\\s*load\\s+\\w+\\s*%\\}", "");
        content = content.replaceAll("\\{%\\s*static\\s+'([^']+)'\\s*%\\}", "/$1");
        content = content.replaceAll("\\{%\\s*static\\s+\"([^\"]+)\"\\s*%\\}", "/$1");
        content = content.replaceAll("\\{\\{\\s*WA_URL\\s*\\}\\}", "/api/wa");

        for (int pass = 0; pass < 5; pass++) {
            content = resolveIncludes(content);
        }
        return content;
    }

    private String resolveIncludes(String content) {
        Pattern p = Pattern.compile("\\{%\\s*include\\s+['\"]([^'\"]+)['\"]\\s*%\\}");
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String partialPath = m.group(1);
            Path partialFile = Paths.get(templatesRoot, partialPath);
            String partialContent;
            try {
                partialContent = Files.readString(partialFile);
            } catch (IOException e) {
                partialContent = "<!-- MISSING: " + partialPath + " -->";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(partialContent));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
