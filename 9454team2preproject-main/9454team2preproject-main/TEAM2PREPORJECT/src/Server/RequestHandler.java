package Server;

import Server.controller.*;
import Server.util.*;
import org.w3c.dom.*;

public class RequestHandler {

    private AccountController accountController;
    private ProductController productController;
    private TransactionController transactionController;

    public RequestHandler() {
        this.accountController = new AccountController();
        this.productController = new ProductController();
        this.transactionController = new TransactionController();
    }

    public String processRequest(String xmlRequest) {
        try {
            // Parse XML request...
            Document doc = XMLParser.parseXMLString(xmlRequest);
            Element root = doc.getDocumentElement();

            // Extract action and username...
            String action = XMLParser.getTagValue(root, "action");
            String username = XMLParser.getTagValue(root, "username");

            // Get data element...
            NodeList dataNodes = root.getElementsByTagName("data");
            Element dataElement = null;
            if (dataNodes.getLength() > 0) {
                dataElement = (Element) dataNodes.item(0);
            }

            // Route to handler...
            String response = " ";

            return response;

        } catch (Exception e) {
            ServerLogger.logError("Failed to process request: " + e.getMessage());
            return XMLParser.createResponse("ERROR", "Invalid request format");
        }
    }
}
