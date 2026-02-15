package Server;

import Server.controller.AccountController;
import Server.controller.ProductController;
import Server.controller.TransactionController;
import Server.util.ServerLogger;
import Server.util.XMLParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * RequestHandler — parses each client XML request and routes to the
 * correct controller method.
 *
 * Expected XML request format:
 * <request>
 *   <action>ACTION_NAME</action>
 *   <username>someuser</username>
 *   <data>
 *     <!-- action-specific fields -->
 *   </data>
 * </request>
 */
public class RequestHandler {

    private final AccountController     accountController;
    private final ProductController     productController;
    private final TransactionController transactionController;

    public RequestHandler() {
        this.accountController     = new AccountController();
        this.productController     = new ProductController();
        this.transactionController = new TransactionController();
    }

    // -------------------------------------------------------
    // Main entry point — called by ClientHandler for every request
    // -------------------------------------------------------
    public String processRequest(String xmlRequest) {
        try {
            Document doc  = XMLParser.parseXMLString(xmlRequest);
            Element  root = doc.getDocumentElement();

            String  action   = XMLParser.getTagValue(root, "action");
            String  username = XMLParser.getTagValue(root, "username");

            // Safely extract <data> element (may not exist for some requests)
            Element dataElement = null;
            NodeList dataNodes  = root.getElementsByTagName("data");
            if (dataNodes.getLength() > 0) {
                dataElement = (Element) dataNodes.item(0);
            }

            if (action == null || action.isEmpty()) {
                return XMLParser.createResponse("ERROR", "Missing <action> in request.");
            }

            // -----------------------------------------------
            // Route to correct handler
            // -----------------------------------------------
            switch (action) {

                // --- Account operations ---
                case "LOGIN":
                    return accountController.login(username, dataElement);

                case "REGISTER":
                    return accountController.register(username, dataElement);

                case "FETCH_ALL_USERS":
                    return accountController.fetchAllUsers(username);

                case "UPDATE_USER_STATUS":
                    return accountController.updateUserStatus(username, dataElement);

                case "DELETE_USER":
                    return accountController.deleteUser(username, dataElement);

                case "CHANGE_PASSWORD":
                    return accountController.changePassword(username, dataElement);

                case "FETCH_LOGS":
                    return accountController.fetchLogs(username);

                // --- Product operations ---
                case "ADD_PRODUCT":
                    return productController.addProduct(username, dataElement);

                case "FETCH_ALL_PRODUCTS":
                    return productController.fetchAllProducts(username);

                case "FETCH_SELLER_PRODUCTS":
                    return productController.fetchSellerProducts(username);

                case "UPDATE_PRODUCT":
                    return productController.updateProduct(username, dataElement);

                case "DELETE_PRODUCT":
                    return productController.deleteProduct(username, dataElement);

                case "SEARCH_PRODUCTS":
                    return productController.searchProducts(username, dataElement);

                case "BUY_PRODUCT":
                    return productController.buyProduct(username, dataElement);

                // --- Transaction operations ---
                case "FETCH_MY_PURCHASES":
                    return transactionController.fetchMyPurchases(username);

                case "FETCH_MY_SALES":
                    return transactionController.fetchMySales(username);

                case "FETCH_ALL_TRANSACTIONS":
                    return transactionController.fetchAllTransactions(username);

                default:
                    ServerLogger.logError("Unknown action: " + action
                            + " from user: " + username);
                    return XMLParser.createResponse("ERROR",
                            "Unknown action: " + action);
            }

        } catch (Exception e) {
            ServerLogger.logError("Failed to process request: " + e.getMessage());
            return XMLParser.createResponse("ERROR",
                    "Invalid request format: " + e.getMessage());
        }
    }
}