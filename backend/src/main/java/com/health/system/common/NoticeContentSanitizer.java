package com.health.system.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class NoticeContentSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("span", "figure", "figcaption")
            .addAttributes(":all", "style")
            .addAttributes("img", "width", "height", "loading")
            .addProtocols("img", "src", "http", "https", "data");

    private NoticeContentSanitizer() {
    }

    public static String sanitizeRichHtml(String html) {
        if (html == null) {
            throw BusinessException.badRequest("公告内容不能为空");
        }

        String cleaned = Jsoup.clean(html, SAFELIST).trim();
        String plainText = Jsoup.parse(cleaned).text().trim();
        boolean hasImage = cleaned.contains("<img");
        if (plainText.isEmpty() && !hasImage) {
            throw BusinessException.badRequest("公告内容不能为空");
        }
        return cleaned;
    }
}
