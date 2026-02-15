package Client.util;

import java.io.*;
import java.net.Socket;

/**
 * SocketClient — shared utility for sending XML requests to the server
 * and receiving XML responses.
 *
 * Usage:
 *   String response = SocketClient.sendRequest(xmlString);
 */
public class SocketClient {

    private static final String SERVER_HOST = "localhost";
    private static final int    SERVER_PORT = 5000;  // Must match Server.java

    /**
     * Sends an XML request string to the server.
     * Returns the server's XML response string, or an error XML string on failure.
     */
    public static String sendRequest(String xmlRequest) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {
            // Send request line by line (server reads until </request>)
            out.println(xmlRequest);

            // Read the full response (server sends one line)
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString().trim();

        } catch (IOException e) {
            return buildErrorResponse("Connection failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // XML Request Builder helpers
    // -------------------------------------------------------

    /**
     * Builds a basic request XML with no <data> block.
     * Use for: FETCH_ALL_PRODUCTS, FETCH_MY_PURCHASES, etc.
     */
    public static String buildRequest(String action, String username) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<request>\n"
                + "  <action>" + action + "</action>\n"
                + "  <username>" + escapeXML(username) + "</username>\n"
                + "</request>";
    }

    /**
     * Builds a request XML with a custom <data> block.
     * dataContent should be the inner XML, e.g. "<password>abc</password>"
     */
    public static String buildRequest(String action, String username, String dataContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<request>\n"
                + "  <action>" + action + "</action>\n"
                + "  <username>" + escapeXML(username) + "</username>\n"
                + "  <data>\n"
                + dataContent + "\n"
                + "  </data>\n"
                + "</request>";
    }

    // -------------------------------------------------------
    // Response parser helpers
    // -------------------------------------------------------

    /** Returns the text inside <status> from a response XML */
    public static String getStatus(String responseXML) {
        return extractTag(responseXML, "status");
    }

    /** Returns the text inside <message> from a response XML */
    public static String getMessage(String responseXML) {
        return extractTag(responseXML, "message");
    }

    /** Returns true if <status>SUCCESS</status> */
    public static boolean isSuccess(String responseXML) {
        return "SUCCESS".equals(getStatus(responseXML));
    }

    /**
     * Returns the raw content inside <data>...</data> from a response.
     * Useful when the data block contains nested XML.
     */
    public static String getDataBlock(String responseXML) {
        int start = responseXML.indexOf("<data>");
        int end   = responseXML.lastIndexOf("</data>");
        if (start == -1 || end == -1) return "";
        return responseXML.substring(start + 6, end).trim();
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------

    private static String extractTag(String xml, String tagName) {
        String open  = "<"  + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end   = xml.indexOf(close);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + open.length(), end).trim();
    }

    private static String buildErrorResponse(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<response>"
                + "<status>ERROR</status>"
                + "<message>" + escapeXML(message) + "</message>"
                + "</response>";
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