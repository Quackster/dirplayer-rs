package com.dirplayer.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML node for Lingo XML parsing.
 * Port of Rust XmlNode struct.
 */
public class XmlNode {
    public int id;
    public String name;
    public String value;
    public Map<String, String> attributes;
    public List<Integer> childNodes;  // IDs of child nodes
    public int parentNode;  // ID of parent node
    public XmlNodeType nodeType;

    public XmlNode() {
        this.id = 0;
        this.name = "";
        this.value = "";
        this.attributes = new HashMap<>();
        this.childNodes = new ArrayList<>();
        this.parentNode = 0;
        this.nodeType = XmlNodeType.Element;
    }

    public XmlNode(int id, String name) {
        this.id = id;
        this.name = name;
        this.value = "";
        this.attributes = new HashMap<>();
        this.childNodes = new ArrayList<>();
        this.parentNode = 0;
        this.nodeType = XmlNodeType.Element;
    }

    public enum XmlNodeType {
        Element,
        Text,
        Comment,
        CData,
        Document
    }
}
