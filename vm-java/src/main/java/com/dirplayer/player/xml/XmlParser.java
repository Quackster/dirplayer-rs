package com.dirplayer.player.xml;

import com.dirplayer.player.XmlDocument;
import com.dirplayer.SimpleLogger;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser for Director Lingo XML objects.
 * Port of Rust XmlParser struct.
 */
public class XmlParser {
    private static final SimpleLogger logger = SimpleLogger.getLogger(XmlParser.class);

    private int nextXmlId;
    private Map<Integer, XmlNode> nodes;

    public XmlParser(int startId) {
        this.nextXmlId = startId;
        this.nodes = new HashMap<>();
    }

    /**
     * Get the next XML ID after parsing.
     */
    public int getNextXmlId() {
        return nextXmlId;
    }

    /**
     * Parse XML string into a document.
     */
    public static XmlDocument parse(String xml) {
        XmlParser parser = new XmlParser(1);
        ParseResult result = parser.parseXmlContent(xml);
        XmlDocument doc = new XmlDocument();
        doc.content = xml;
        doc.rootElementId = result.rootElementId;
        return doc;
    }

    /**
     * Parse XML content and build node structure.
     */
    public ParseResult parseXmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ParseResult(null, new HashMap<>());
        }

        nodes.clear();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            XmlContentHandler handler = new XmlContentHandler();
            saxParser.parse(new InputSource(new StringReader(content)), handler);

            return new ParseResult(handler.rootElementId, nodes);
        } catch (Exception e) {
            logger.warn("Failed to parse XML: {}", e.getMessage());
            // Return empty result on parse error
            return new ParseResult(null, new HashMap<>());
        }
    }

    private class XmlContentHandler extends DefaultHandler {
        Integer rootElementId = null;
        Stack<Integer> elementStack = new Stack<>();
        StringBuilder currentText = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            // Flush any pending text content
            flushText();

            int elementId = nextXmlId++;
            String name = qName.toLowerCase();

            XmlNode node = new XmlNode(elementId, XmlNodeType.Element, name);

            // Add attributes
            for (int i = 0; i < attributes.getLength(); i++) {
                node.setAttribute(attributes.getQName(i), attributes.getValue(i));
            }

            // Set parent
            if (!elementStack.isEmpty()) {
                int parentId = elementStack.peek();
                node.parentId = parentId;
                XmlNode parent = nodes.get(parentId);
                if (parent != null) {
                    parent.addChild(elementId);
                }
            }

            nodes.put(elementId, node);

            if (rootElementId == null) {
                rootElementId = elementId;
            }

            elementStack.push(elementId);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            flushText();
            if (!elementStack.isEmpty()) {
                elementStack.pop();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            currentText.append(ch, start, length);
        }

        private void flushText() {
            String text = currentText.toString().trim();
            if (!text.isEmpty() && !elementStack.isEmpty()) {
                int parentId = elementStack.peek();
                int textNodeId = nextXmlId++;

                XmlNode textNode = new XmlNode(textNodeId, XmlNodeType.Text, "#text");
                textNode.value = text;
                textNode.parentId = parentId;

                nodes.put(textNodeId, textNode);

                XmlNode parent = nodes.get(parentId);
                if (parent != null) {
                    parent.addChild(textNodeId);
                }
            }
            currentText.setLength(0);
        }
    }

    /**
     * Get all nodes in the parsed document.
     */
    public Map<Integer, XmlNode> getNodes() {
        return nodes;
    }

    /**
     * Find child nodes by name.
     */
    public static List<Integer> findNodesByName(Map<Integer, XmlNode> nodes, int parentId, String name) {
        List<Integer> results = new ArrayList<>();
        XmlNode parent = nodes.get(parentId);
        if (parent == null) {
            return results;
        }

        String searchName = name.toLowerCase();
        for (int childId : parent.childIds) {
            XmlNode child = nodes.get(childId);
            if (child != null && child.name.equalsIgnoreCase(searchName)) {
                results.add(childId);
            }
        }

        return results;
    }

    /**
     * Recursively find all descendant nodes by name.
     */
    public static List<Integer> findAllNodesByName(Map<Integer, XmlNode> nodes, int startId, String name) {
        List<Integer> results = new ArrayList<>();
        findAllNodesByNameRecursive(nodes, startId, name.toLowerCase(), results);
        return results;
    }

    private static void findAllNodesByNameRecursive(Map<Integer, XmlNode> nodes, int nodeId, String name, List<Integer> results) {
        XmlNode node = nodes.get(nodeId);
        if (node == null) {
            return;
        }

        if (node.name.equalsIgnoreCase(name)) {
            results.add(nodeId);
        }

        for (int childId : node.childIds) {
            findAllNodesByNameRecursive(nodes, childId, name, results);
        }
    }

    public static class ParseResult {
        public Integer rootElementId;
        public Map<Integer, XmlNode> nodes;

        public ParseResult(Integer rootElementId, Map<Integer, XmlNode> nodes) {
            this.rootElementId = rootElementId;
            this.nodes = nodes;
        }
    }
}
