package com.dirplayer.player;

import java.util.ArrayList;
import java.util.List;

/**
 * XML document for Lingo XML parsing.
 * Port of Rust XmlDocument struct.
 */
public class XmlDocument {
    public int id;
    public XmlNode rootNode;
    public String sourceText;
    public boolean isValid;
    public String parseError;

    public XmlDocument() {
        this.id = 0;
        this.rootNode = null;
        this.sourceText = "";
        this.isValid = false;
        this.parseError = null;
    }

    public XmlDocument(int id) {
        this.id = id;
        this.rootNode = null;
        this.sourceText = "";
        this.isValid = false;
        this.parseError = null;
    }
}
