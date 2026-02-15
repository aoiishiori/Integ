package Server.controller;

import Server.model.Transaction;
import Server.util.ServerLogger;
import Server.util.XMLParser;
import Server.util.XMLReader;

import java.util.List;

/**
 * Supported actions:
 *   FETCH_MY_PURCHASES  — buyer sees their own orders
 *   FETCH_MY_SALES      — seller sees their own sales
 *   FETCH_ALL_TRANSACTIONS — admin sees everything
 */
public class TransactionController {


    public String fetchMyPurchases(String buyerUsername) {
        List<Transaction> all = XMLReader.readTransactions();

        StringBuilder data = new StringBuilder("<transactions>");
        for (Transaction t : all) {
            if (buyerUsername.equals(t.getBuyerUsername())) {
                data.append(txToXML(t));
            }
        }
        data.append("</transactions>");

        ServerLogger.logTransaction(buyerUsername, "FETCH_MY_PURCHASES", "N/A");
        return XMLParser.createResponseWithData("SUCCESS", "Your purchases.", data.toString());
    }


    public String fetchMySales(String sellerUsername) {
        List<Transaction> all = XMLReader.readTransactions();

        StringBuilder data = new StringBuilder("<transactions>");
        for (Transaction t : all) {
            if (sellerUsername.equals(t.getSellerUsername())) {
                data.append(txToXML(t));
            }
        }
        data.append("</transactions>");

        ServerLogger.logTransaction(sellerUsername, "FETCH_MY_SALES", "N/A");
        return XMLParser.createResponseWithData("SUCCESS", "Your sales.", data.toString());
    }


    public String fetchAllTransactions(String adminUsername) {
        List<Transaction> all = XMLReader.readTransactions();

        StringBuilder data = new StringBuilder("<transactions>");
        for (Transaction t : all) {
            data.append(txToXML(t));
        }
        data.append("</transactions>");

        ServerLogger.logTransaction(adminUsername, "FETCH_ALL_TRANSACTIONS",
                "count=" + all.size());
        return XMLParser.createResponseWithData("SUCCESS",
                "All transactions.", data.toString());
    }

    private String txToXML(Transaction t) {
        return "<transaction>"
                + "<transactionId>" + t.getTransactionId() + "</transactionId>"
                + "<productId>" + t.getProductId() + "</productId>"
                + "<buyerUsername>" + t.getBuyerUsername() + "</buyerUsername>"
                + "<sellerUsername>" + t.getSellerUsername() + "</sellerUsername>"
                + "<quantity>" + t.getQuantity() + "</quantity>"
                + "<timestamp>" + t.getTimestamp() + "</timestamp>"
                + "</transaction>";
    }
}