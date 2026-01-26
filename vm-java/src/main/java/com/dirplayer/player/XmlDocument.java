package com.dirplayer.player;

import java.util.ArrayList;
import java.util.List;

/**
 * XML document for Lingo XML parsing.
 * Port of Rust XmlDocument struct.
 */
public class XmlDocument {
    public int id;
    public Integer rootElementId;  // ID of the root element node
    public String content;
    public boolean ignoreWhite;

    public XmlDocument() {
        this.id = 0;
        this.rootElementId = null;
        this.content = "";
        this.ignoreWhite = false;
    }

    public XmlDocument(int id) {
        this.id = id;
        this.rootElementId = null;
        this.content = "";
        this.ignoreWhite = false;
    }

    public XmlDocument(int id, Integer rootElementId, String content, boolean ignoreWhite) {
        this.id = id;
        this.rootElementId = rootElementId;
        this.content = content;
        this.ignoreWhite = ignoreWhite;
    }

    public Integer getRootElement() {
        return rootElementId;
    }

    public void setRootElement(Integer rootElementId) {
        this.rootElementId = rootElementId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isIgnoreWhite() {
        return ignoreWhite;
    }

    public void setIgnoreWhite(boolean ignoreWhite) {
        this.ignoreWhite = ignoreWhite;
    }
}
