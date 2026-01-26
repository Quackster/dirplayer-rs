package com.dirplayer.player.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML node representing an element in a parsed XML document.
 * Port of Rust XmlNode struct.
 */
public class XmlNode {
    public int id;
    public XmlNodeType nodeType;
    public String name;
    public String value;
    public Map<String, String> attributes;
    public Integer parentId;
    public List<Integer> childIds;

    public XmlNode() {
        this.id = 0;
        this.nodeType = XmlNodeType.Element;
        this.name = "";
        this.value = null;
        this.attributes = new HashMap<>();
        this.parentId = null;
        this.childIds = new ArrayList<>();
    }

    public XmlNode(int id, XmlNodeType nodeType, String name) {
        this.id = id;
        this.nodeType = nodeType;
        this.name = name;
        this.value = null;
        this.attributes = new HashMap<>();
        this.parentId = null;
        this.childIds = new ArrayList<>();
    }

    public void addChild(int childId) {
        childIds.add(childId);
    }

    public void setAttribute(String key, String val) {
        attributes.put(key, val);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }
}
