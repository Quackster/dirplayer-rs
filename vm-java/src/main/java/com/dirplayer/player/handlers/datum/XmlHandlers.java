package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.XmlDocument;
import com.dirplayer.player.xml.XmlNode;
import com.dirplayer.player.xml.XmlNodeType;
import com.dirplayer.player.xml.XmlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handlers for XML datum operations.
 * Port of Rust XmlDatumHandlers.
 */
public class XmlHandlers {

    /**
     * Call handler on XML datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "parsexml":
                return parseXml(player, datumRef, args);
            case "createelement":
                return createElement(player, datumRef, args);
            case "appendchild":
                return appendChild(player, datumRef, args);
            case "tostring":
                return toString(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for XML object");
        }
    }

    /**
     * Get a property from an XML datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.getType() != DatumType.XmlRef) {
            throw new ScriptError("Invalid XML reference");
        }

        int xmlId = datum.getXmlRef();
        return getXmlProperty(player, xmlId, prop);
    }

    /**
     * Set a property on an XML datum.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.getType() != DatumType.XmlRef) {
            throw new ScriptError("Invalid XML reference");
        }

        int xmlId = datum.getXmlRef();
        setXmlProperty(player, xmlId, prop, valueRef);
    }

    /**
     * Parse XML string content.
     */
    private static int parseXml(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("parseXML requires XML string argument");
        }

        Datum parserDatum = player.getDatum(datumRef);
        if (parserDatum.getType() != DatumType.XmlRef) {
            throw new ScriptError("parseXML must be called on XML parser object");
        }

        int parserId = parserDatum.getXmlRef();

        Datum argDatum = player.getDatum(args.get(0));
        if (argDatum.isVoid()) {
            throw new ScriptError("parseXML argument is Void - expected XML string");
        }
        if (!argDatum.isString()) {
            throw new ScriptError("parseXML requires string argument, got: " + argDatum.typeStr());
        }

        String xmlString = argDatum.stringValue();

        // Parse the XML content using XmlParser
        XmlParser parser = new XmlParser(player.nextXmlId);
        XmlParser.ParseResult result = parser.parseXmlContent(xmlString);

        // Update next XML ID in player
        player.nextXmlId = parser.getNextXmlId();

        // Add all parsed nodes to the player's xml_nodes
        // Need to convert from xml package XmlNode to player's storage format
        for (Map.Entry<Integer, com.dirplayer.player.xml.XmlNode> entry : result.nodes.entrySet()) {
            com.dirplayer.player.xml.XmlNode srcNode = entry.getValue();
            // Store directly since DirPlayer uses xml.XmlNode
            player.xmlNodes.put(entry.getKey(), srcNode);
        }

        // Update the parser document with the parsed content
        XmlDocument parserDoc = player.xmlDocuments.get(parserId);
        if (parserDoc != null) {
            parserDoc.setRootElement(result.rootElementId);
            parserDoc.setContent(xmlString);
        } else {
            // Create new document if it doesn't exist
            XmlDocument xmlDoc = new XmlDocument(parserId, result.rootElementId, xmlString, false);
            player.xmlDocuments.put(parserId, xmlDoc);
        }

        // parseXML returns Void in Lingo
        return 0; // Void
    }

    /**
     * Create a new XML element.
     */
    private static int createElement(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("createElement requires element name argument");
        }

        Datum docDatum = player.getDatum(datumRef);
        if (docDatum.getType() != DatumType.XmlRef) {
            throw new ScriptError("createElement must be called on XML document");
        }

        // Get the element name from arguments
        Datum nameDatum = player.getDatum(args.get(0));
        String elementName;
        if (nameDatum.isString()) {
            elementName = nameDatum.stringValue();
        } else if (nameDatum.isSymbol()) {
            elementName = nameDatum.symbolValue();
        } else {
            throw new ScriptError("createElement requires string element name");
        }

        // Create a new XML element node
        int elementId = player.nextXmlId++;

        XmlNode elementNode = new XmlNode(elementId, XmlNodeType.Element, elementName);
        elementNode.value = null;
        elementNode.attributes = new HashMap<>();
        elementNode.parentId = null;
        elementNode.childIds = new ArrayList<>();

        player.xmlNodes.put(elementId, elementNode);

        return player.allocDatum(Datum.ofXmlRef(elementId));
    }

    /**
     * Append a child node to a parent node/document.
     */
    private static int appendChild(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("appendChild requires child node argument");
        }

        Datum parentDatum = player.getDatum(datumRef);
        if (parentDatum.getType() != DatumType.XmlRef) {
            throw new ScriptError("appendChild must be called on XML node");
        }

        int parentId = parentDatum.getXmlRef();

        Datum childDatum = player.getDatum(args.get(0));
        if (childDatum.getType() != DatumType.XmlRef) {
            throw new ScriptError("appendChild requires XML node argument");
        }

        int childId = childDatum.getXmlRef();

        // Check if parent is a document
        XmlDocument doc = player.xmlDocuments.get(parentId);
        if (doc != null) {
            // Appending to document - set as root element
            doc.setRootElement(childId);
        } else {
            // Appending to a node
            XmlNode parentNode = player.xmlNodes.get(parentId);
            if (parentNode == null) {
                throw new ScriptError("Parent node " + parentId + " not found");
            }

            // Add to children list if not already there
            if (!parentNode.childIds.contains(childId)) {
                parentNode.childIds.add(childId);
            }

            // Update child's parent reference
            XmlNode childNode = player.xmlNodes.get(childId);
            if (childNode != null) {
                childNode.parentId = parentId;
            }
        }

        // Return the child node (Director appendChild returns the appended child)
        return args.get(0);
    }

    /**
     * Convert XML to string.
     */
    private static int toString(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.getType() != DatumType.XmlRef) {
            throw new ScriptError("toString must be called on XML object");
        }

        int xmlId = datum.getXmlRef();

        // Check if it's a document
        XmlDocument doc = player.xmlDocuments.get(xmlId);
        Integer rootId;
        if (doc != null) {
            rootId = doc.getRootElement();
        } else {
            // If it's a node, start from that node
            rootId = xmlId;
        }

        if (rootId != null) {
            String xmlString = serializeNode(player, rootId);
            return player.allocDatum(Datum.ofString(xmlString));
        } else {
            return player.allocDatum(Datum.ofString(""));
        }
    }

    /**
     * Serialize an XML node to string.
     */
    private static String serializeNode(DirPlayer player, int nodeId) {
        XmlNode node = player.xmlNodes.get(nodeId);
        if (node == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();

        switch (node.nodeType) {
            case Element:
                xml.append("<").append(node.name);

                // Add attributes
                for (Map.Entry<String, String> attr : node.attributes.entrySet()) {
                    xml.append(" ").append(attr.getKey())
                       .append("=\"").append(escapeXml(attr.getValue())).append("\"");
                }

                // Check if has children
                if (node.childIds.isEmpty()) {
                    xml.append(" />");
                } else {
                    xml.append(">");

                    // Serialize children
                    for (Integer childId : node.childIds) {
                        xml.append(serializeNode(player, childId));
                    }

                    xml.append("</").append(node.name).append(">");
                }
                break;

            case Text:
                if (node.value != null) {
                    xml.append(escapeXml(node.value));
                }
                break;

            case CData:
                if (node.value != null) {
                    xml.append("<![CDATA[").append(node.value).append("]]>");
                }
                break;

            case Comment:
                if (node.value != null) {
                    xml.append("<!--").append(node.value).append("-->");
                }
                break;

            default:
                // Other node types return empty string
                break;
        }

        return xml.toString();
    }

    /**
     * Escape XML special characters.
     */
    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Get an XML property.
     */
    private static int getXmlProperty(DirPlayer player, int xmlId, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "firstchild": {
                // Check if it's a document
                XmlDocument doc = player.xmlDocuments.get(xmlId);
                if (doc != null) {
                    Integer rootId = doc.getRootElement();
                    if (rootId != null) {
                        return player.allocDatum(Datum.ofXmlRef(rootId));
                    }
                    return player.allocDatum(Datum.VOID);
                }

                // Check if it's a node
                XmlNode node = player.xmlNodes.get(xmlId);
                if (node != null) {
                    List<Integer> children = getNodeChildren(player, xmlId);
                    if (!children.isEmpty()) {
                        return children.get(0);
                    }
                }
                return player.allocDatum(Datum.VOID);
            }

            case "lastchild": {
                XmlNode node = player.xmlNodes.get(xmlId);
                if (node != null) {
                    List<Integer> children = getNodeChildren(player, xmlId);
                    if (!children.isEmpty()) {
                        return children.get(children.size() - 1);
                    }
                }
                return player.allocDatum(Datum.VOID);
            }

            case "childnodes": {
                // Check if it's a document
                XmlDocument doc = player.xmlDocuments.get(xmlId);
                if (doc != null) {
                    Integer rootId = doc.getRootElement();
                    if (rootId != null) {
                        int rootRef = player.allocDatum(Datum.ofXmlRef(rootId));
                        List<Integer> children = new ArrayList<>();
                        children.add(rootRef);
                        return player.allocDatum(Datum.ofList(DatumType.XmlChildNodes, children, false));
                    } else {
                        return player.allocDatum(Datum.ofList(DatumType.XmlChildNodes, new ArrayList<>(), false));
                    }
                }

                // For regular nodes, get their children
                List<Integer> children = getNodeChildren(player, xmlId);
                return player.allocDatum(Datum.ofList(DatumType.XmlChildNodes, children, false));
            }

            case "nodename": {
                XmlNode node = player.xmlNodes.get(xmlId);
                if (node != null) {
                    return player.allocDatum(Datum.ofString(cleanNodeName(node.name)));
                } else {
                    return player.allocDatum(Datum.ofString("#document"));
                }
            }

            case "nodevalue": {
                XmlNode node = player.xmlNodes.get(xmlId);
                if (node != null && node.value != null) {
                    return player.allocDatum(Datum.ofString(node.value));
                }
                return player.allocDatum(Datum.VOID);
            }

            case "attributes": {
                // Return a special XmlRef for attributes (offset by 10000)
                int attrId = xmlId + 10000;
                return player.allocDatum(Datum.ofXmlRef(attrId));
            }

            case "ignorewhite": {
                XmlDocument doc = player.xmlDocuments.get(xmlId);
                boolean ignore = doc != null && doc.isIgnoreWhite();
                return player.allocDatum(Datum.ofInt(ignore ? 1 : 0));
            }

            default: {
                // Check if this is an attribute access (xmlId > 10000)
                if (xmlId > 10000) {
                    int nodeId = xmlId - 10000;
                    XmlNode node = player.xmlNodes.get(nodeId);
                    if (node != null) {
                        String value = node.attributes.get(prop.toLowerCase());
                        return player.allocDatum(Datum.ofString(value != null ? value : ""));
                    }
                    return player.allocDatum(Datum.ofString(""));
                }
                throw new ScriptError("Unknown XML property: " + prop);
            }
        }
    }

    /**
     * Set an XML property.
     */
    private static void setXmlProperty(DirPlayer player, int xmlId, String prop, int valueRef) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ignorewhite": {
                int ignoreValue = player.getDatum(valueRef).intValue();
                XmlDocument doc = player.xmlDocuments.get(xmlId);
                if (doc != null) {
                    doc.setIgnoreWhite(ignoreValue != 0);
                }
                break;
            }

            default: {
                // Check if this is setting an attribute (xmlId > 10000)
                if (xmlId > 10000) {
                    int nodeId = xmlId - 10000;
                    String valueStr = player.getDatum(valueRef).stringValue();

                    XmlNode node = player.xmlNodes.get(nodeId);
                    if (node != null) {
                        node.attributes.put(prop, valueStr);
                    } else {
                        throw new ScriptError("Node " + nodeId + " not found");
                    }
                } else {
                    throw new ScriptError("Cannot set XML property: " + prop);
                }
            }
        }
    }

    /**
     * Get children of an XML node (filtered by ignore_white setting).
     */
    private static List<Integer> getNodeChildren(DirPlayer player, int nodeId) {
        XmlNode node = player.xmlNodes.get(nodeId);
        if (node == null) {
            return new ArrayList<>();
        }

        List<Integer> children = new ArrayList<>();
        boolean ignoreWhite = shouldIgnoreWhitespace(player, nodeId);

        for (Integer childId : node.childIds) {
            XmlNode childNode = player.xmlNodes.get(childId);
            if (childNode != null) {
                boolean shouldInclude = true;

                if (childNode.nodeType == XmlNodeType.Text) {
                    String text = childNode.value;
                    if (text != null && text.trim().isEmpty() && ignoreWhite) {
                        shouldInclude = false;
                    }
                }

                if (shouldInclude) {
                    children.add(player.allocDatum(Datum.ofXmlRef(childId)));
                }
            }
        }

        return children;
    }

    /**
     * Check if whitespace should be ignored for a node.
     */
    private static boolean shouldIgnoreWhitespace(DirPlayer player, int nodeId) {
        for (Map.Entry<Integer, XmlDocument> entry : player.xmlDocuments.entrySet()) {
            if (nodeBelongsToDocument(player, nodeId, entry.getKey())) {
                return entry.getValue().isIgnoreWhite();
            }
        }
        return false;
    }

    /**
     * Check if a node belongs to a document.
     */
    private static boolean nodeBelongsToDocument(DirPlayer player, int nodeId, int docId) {
        XmlDocument doc = player.xmlDocuments.get(docId);
        if (doc != null) {
            Integer rootId = doc.getRootElement();
            if (rootId != null) {
                return isDescendantOf(player, nodeId, rootId) || nodeId == rootId;
            }
        }
        return false;
    }

    /**
     * Check if a node is a descendant of another node.
     */
    private static boolean isDescendantOf(DirPlayer player, int nodeId, int ancestorId) {
        XmlNode node = player.xmlNodes.get(nodeId);
        if (node != null) {
            Integer parentId = node.parentId;
            if (parentId != null && parentId != 0) {
                if (parentId == ancestorId) {
                    return true;
                } else {
                    return isDescendantOf(player, parentId, ancestorId);
                }
            }
        }
        return false;
    }

    /**
     * Clean node name (remove quotes and lowercase).
     */
    private static String cleanNodeName(String name) {
        return name.replace("\"", "").toLowerCase();
    }

    /**
     * Find all nodes with a specific name (for XPath-like queries).
     */
    public static List<Integer> findNodesByName(DirPlayer player, int startNodeId, String targetName) {
        List<Integer> results = new ArrayList<>();

        // Check if the start node is a document, if so get the root element
        XmlDocument doc = player.xmlDocuments.get(startNodeId);
        int startId;
        if (doc != null) {
            Integer rootId = doc.getRootElement();
            if (rootId != null) {
                startId = rootId;
            } else {
                return results;
            }
        } else {
            startId = startNodeId;
        }

        // Recursively search for matching nodes
        searchNodesRecursive(player, startId, targetName, results);

        return results;
    }

    /**
     * Recursive helper for finding nodes by name.
     */
    private static void searchNodesRecursive(DirPlayer player, int nodeId, String targetName, List<Integer> results) {
        XmlNode node = player.xmlNodes.get(nodeId);
        if (node == null) {
            return;
        }

        if (node.name.equals(targetName)) {
            results.add(player.allocDatum(Datum.ofXmlRef(nodeId)));
        }

        // Recurse into children
        for (Integer childId : node.childIds) {
            searchNodesRecursive(player, childId, targetName, results);
        }
    }
}
