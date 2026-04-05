package com.helpmanual.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class HtmlSanitizer {

    private static final Safelist WHITELIST = Safelist.relaxed()
        .addTags("div", "span", "pre", "code", "blockquote", "hr", "br", "table", "thead", "tbody", "tr", "th", "td", "caption", "figure", "figcaption", "details", "summary")
        .addAttributes("div", "class", "style", "data-w-e-type")
        .addAttributes("span", "class", "style")
        .addAttributes("pre", "class")
        .addAttributes("code", "class")
        .addAttributes("img", "src", "alt", "title", "width", "height", "style", "data-href")
        .addAttributes("video", "src", "controls", "width", "height", "poster", "preload", "style")
        .addAttributes("source", "src", "type")
        .addAttributes("a", "href", "title", "target", "rel")
        .addAttributes("td", "colspan", "rowspan", "style")
        .addAttributes("th", "colspan", "rowspan", "style")
        .addAttributes("table", "class", "style")
        .addAttributes(":all", "id")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "/api/")
        .addProtocols("video", "src", "http", "https", "/api/")
        .preserveRelativeLinks(true);

    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, "", WHITELIST);
    }
}
