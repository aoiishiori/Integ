package Server.util;

import Server.model.Product;
import Server.model.Account;
import Server.model.Transaction;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class XMLReader {

    private static final String DATA_DIR = "data/";
    private static final String ACCOUNTS_FILE = DATA_DIR + "Accounts.xml";
    private static final String PRODUCTS_FILE = DATA_DIR + "Products.xml";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "Transactions.xml";

    public synchronized static List<Account> readAccounts() {
        List<Account> accounts = new ArrayList<>();

        try {
            File file = new File(ACCOUNTS_FILE);
            if (!file.exists()) {
                return accounts;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Account");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    Account account = new Account();
                    account.setAccountId(getElementTextContent(element, "accountId"));
                    account.setUsername(getElementTextContent(element, "username"));
                    account.setPassword(getElementTextContent(element, "password"));
                    account.setRole(getElementTextContent(element, "role"));
                    account.setStatus(getElementTextContent(element, "status"));

                    accounts.add(account);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read accounts: " + e.getMessage());
        }

        return accounts;
    }

    public synchronized static List<Product> readProducts() {
        List<Product> products = new ArrayList<>();

        try {
            File file = new File(PRODUCTS_FILE);
            if (!file.exists()) {
                return products;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Product");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    Product product = new Product();
                    product.setProductId(getElementTextContent(element, "productId"));
                    product.setSellerUsername(getElementTextContent(element, "sellerUsername"));
                    product.setName(getElementTextContent(element, "name"));
                    product.setCategory(getElementTextContent(element, "category"));
                    product.setOriginalPrice(parseDouble(getElementTextContent(element, "originalPrice")));
                    product.setDiscountedPrice(parseDouble(getElementTextContent(element, "discountedPrice")));
                    product.setAvailableQuantity(parseInt(getElementTextContent(element, "availableQuantity")));
                    product.setExpiryDate(getElementTextContent(element, "expiryDate"));
                    product.setStatus(getElementTextContent(element, "status"));

                    products.add(product);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read products: " + e.getMessage());
        }

        return products;
    }

    public synchronized static List<Transaction> readTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        try {
            File file = new File(TRANSACTIONS_FILE);
            if (!file.exists()) {
                return transactions;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("Transaction");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    Transaction transaction = new Transaction();
                    transaction.setTransactionId(getElementTextContent(element, "transactionId"));
                    transaction.setProductId(getElementTextContent(element, "productId"));
                    transaction.setBuyerUsername(getElementTextContent(element, "buyerUsername"));
                    transaction.setSellerUsername(getElementTextContent(element, "sellerUsername"));
                    transaction.setQuantity(parseInt(getElementTextContent(element, "quantity")));
                    transaction.setTimestamp(getElementTextContent(element, "timestamp"));

                    transactions.add(transaction);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read transactions: " + e.getMessage());
        }

        return transactions;
    }

    private static String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getFirstChild() != null) {
                return node.getFirstChild().getNodeValue();
            }
        }
        return "";
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}