package com.webmagic.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Source Enum
 *
 * 4 knowledge/article sources covering 4 different parsing techniques.
 *
 * JD Coverage: Java basics (enum with constructor, getter, static lookup)
 *
 * @author webmagic-demo
 */
@Getter
@AllArgsConstructor
public enum SourceType {

    /** Juejin - POST JSON API */
    JUEJIN("Juejin", "https://juejin.cn"),

    /** SegmentFault - Pure CSS selector */
    SEGMENTFAULT("SegmentFault", "https://segmentfault.com"),

    /** GitHub Trending - CSS + Regex */
    GITHUB("GitHub Trending", "https://github.com/trending"),

    /** OsChina - Pure XPath */
    OSCHINA("OsChina", "https://www.oschina.net");

    private final String displayName;
    private final String baseUrl;

    public static SourceType findByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (SourceType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}