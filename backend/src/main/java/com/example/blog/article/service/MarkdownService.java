package com.example.blog.article.service;

import java.util.List;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    private static final Pattern LATIN_WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create()
    );

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(true)
            .build();
    private final Safelist safelist = Safelist.relaxed()
            .addTags("table", "thead", "tbody", "tfoot", "tr", "th", "td", "del")
            .addAttributes("th", "align")
            .addAttributes("td", "align")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto");

    public RenderedMarkdown render(String markdown) {
        String source = markdown == null ? "" : markdown;
        String rawHtml = renderer.render(parser.parse(source));
        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleanHtml = Jsoup.clean(rawHtml, "", safelist, outputSettings);
        Document document = Jsoup.parseBodyFragment(cleanHtml);
        for (Element link : document.select("a[href]")) {
            if (link.hasAttr("href") && isExternal(link.attr("href"))) {
                link.attr("rel", "noopener noreferrer nofollow");
            }
        }
        String html = document.body().html();
        String plain = document.text();
        int wordCount = countWords(plain);
        int readingMinutes = wordCount == 0 ? 0 : Math.max(1, (int) Math.ceil(wordCount / 300.0));
        return new RenderedMarkdown(html, plain, wordCount, readingMinutes);
    }

    private boolean isExternal(String href) {
        return href.startsWith("http://") || href.startsWith("https://");
    }

    private int countWords(String text) {
        int cjkCount = 0;
        StringBuilder nonCjk = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                cjkCount++;
                nonCjk.append(' ');
            } else {
                nonCjk.appendCodePoint(codePoint);
            }
        }
        long latinCount = LATIN_WORD.matcher(nonCjk).results().count();
        return Math.toIntExact(cjkCount + latinCount);
    }

    public record RenderedMarkdown(
            String html,
            String plain,
            int wordCount,
            int readingMinutes
    ) {
    }
}
