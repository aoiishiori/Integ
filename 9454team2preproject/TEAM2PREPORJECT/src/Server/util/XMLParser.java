package Server.util;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

public class XMLParser {

    public static Document parseXMLString(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
        Document doc = builder.parse(is);
        doc.getDocumentElement().normalize();

        return doc;
    }

    public static String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getFirstChild() != null) {
                return node.getFirstChild().getNodeValue();
            }
        }
        return null;
    }

    public static String createResponse(String status, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<response>");
        sb.append("<status>").append(escapeXML(status)).append("</status>");
        sb.append("<message>").append(escapeXML(message)).append("</message>");
        sb.append("</response>");
        return sb.toString();
    }

    public static String createResponseWithData(String status, String message, String data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<response>");
        sb.append("<status>").append(escapeXML(status)).append("</status>");
        sb.append("<message>").append(escapeXML(message)).append("</message>");
        sb.append("<data>").append(data).append("</data>");
        sb.append("</response>");
        return sb.toString();
    }

    private static String escapeXML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}