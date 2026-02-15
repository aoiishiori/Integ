package Client.BUYER.MODEL;

import Client.util.SocketClient;
import java.util.ArrayList;
import java.util.List;

/**
 * BuyerModel — MVC Model for the Buyer dashboard.
 *
 * Each product is returned as a String array:
 *   [0] productId
 *   [1] sellerUsername
 *   [2] name
 *   [3] category
 *   [4] originalPrice
 *   [5] discountedPrice
 *   [6] availableQuantity
 *   [7] expiryDate
 *   [8] status
 */
public class BuyerModel {

    // -------------------------------------------------------
    // Fetch all available products
    // -------------------------------------------------------
    public List<String[]> fetchAllProducts(String username) {
        String request  = SocketClient.buildRequest("FETCH_ALL_PRODUCTS", username);
        String response = SocketClient.sendRequest(request);
        return parseProducts(response);
    }

    // -------------------------------------------------------
    // Search products by keyword (name or category)
    // -------------------------------------------------------
    public List<String[]> searchProducts(String username, String keyword) {
        String data     = "    <keyword>" + keyword + "</keyword>";
        String request  = SocketClient.buildRequest("SEARCH_PRODUCTS", username, data);
        String response = SocketClient.sendRequest(request);
        return parseProducts(response);
    }

    // -------------------------------------------------------
    // Buy a product
    // Returns [status, message] — status is "SUCCESS" or "FAILED" etc.
    // -------------------------------------------------------
    public String[] buyProduct(String username, String productId, int quantity) {
        String data = "    <productId>" + productId + "</productId>\n"
                + "    <quantity>" + quantity + "</quantity>";
        String request  = SocketClient.buildRequest("BUY_PRODUCT", username, data);
        String response = SocketClient.sendRequest(request);
        return new String[]{
                SocketClient.getStatus(response),
                SocketClient.getMessage(response)
        };
    }

    // -------------------------------------------------------
    // Fetch purchase history for this buyer
    // Each transaction: [transactionId, productId, sellerUsername, quantity, timestamp]
    // -------------------------------------------------------
    public List<String[]> fetchMyPurchases(String username) {
        String request  = SocketClient.buildRequest("FETCH_MY_PURCHASES", username);
        String response = SocketClient.sendRequest(request);
        return parseTransactions(response);
    }

    // -------------------------------------------------------
    // Parsers
    // -------------------------------------------------------

    private List<String[]> parseProducts(String response) {
        List<String[]> products = new ArrayList<>();
        if (!SocketClient.isSuccess(response)) return products;

        String dataBlock = SocketClient.getDataBlock(response);
        String[] blocks  = dataBlock.split("</product>");

        for (String block : blocks) {
            if (!block.contains("<product>")) continue;
            String inner = block.substring(block.indexOf("<product>") + 9);

            products.add(new String[]{
                    extractTag(inner, "productId"),
                    extractTag(inner, "sellerUsername"),
                    extractTag(inner, "n"),             // name tag
                    extractTag(inner, "category"),
                    extractTag(inner, "originalPrice"),
                    extractTag(inner, "discountedPrice"),
                    extractTag(inner, "availableQuantity"),
                    extractTag(inner, "expiryDate"),
                    extractTag(inner, "status")
            });
        }
        return products;
    }

    private List<String[]> parseTransactions(String response) {
        List<String[]> txList = new ArrayList<>();
        if (!SocketClient.isSuccess(response)) return txList;

        String dataBlock = SocketClient.getDataBlock(response);
        String[] blocks  = dataBlock.split("</transaction>");

        for (String block : blocks) {
            if (!block.contains("<transaction>")) continue;
            String inner = block.substring(block.indexOf("<transaction>") + 13);

            txList.add(new String[]{
                    extractTag(inner, "transactionId"),
                    extractTag(inner, "productId"),
                    extractTag(inner, "sellerUsername"),
                    extractTag(inner, "quantity"),
                    extractTag(inner, "timestamp")
            });
        }
        return txList;
    }

    private String extractTag(String xml, String tagName) {
        String open  = "<"  + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end   = xml.indexOf(close);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + open.length(), end).trim();
    }
}