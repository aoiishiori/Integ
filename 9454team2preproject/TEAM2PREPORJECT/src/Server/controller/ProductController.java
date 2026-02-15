package Server.controller;

import Server.model.Product;
import Server.model.Transaction;
import Server.util.ServerLogger;
import Server.util.XMLParser;
import Server.util.XMLReader;
import Server.util.XMLWriter;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *   ADD_PRODUCT, FETCH_ALL_PRODUCTS, FETCH_SELLER_PRODUCTS,
 *   UPDATE_PRODUCT, DELETE_PRODUCT, SEARCH_PRODUCTS, BUY_PRODUCT
 */
public class ProductController {

    // Global lock for purchase operations — prevents overselling
    private static final Object purchaseLock = new Object();

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");


    public String addProduct(String sellerUsername, Element dataElement) {
        Product p = new Product();
        p.setProductId("PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setSellerUsername(sellerUsername);
        p.setName(XMLParser.getTagValue(dataElement, "name"));
        p.setCategory(XMLParser.getTagValue(dataElement, "category"));
        p.setOriginalPrice(toDouble(XMLParser.getTagValue(dataElement, "originalPrice")));
        p.setDiscountedPrice(toDouble(XMLParser.getTagValue(dataElement, "discountedPrice")));
        p.setAvailableQuantity(toInt(XMLParser.getTagValue(dataElement, "availableQuantity")));
        p.setExpiryDate(XMLParser.getTagValue(dataElement, "expiryDate"));
        p.setStatus("AVAILABLE");

        List<Product> products = XMLReader.readProducts();
        products.add(p);
        XMLWriter.writeProducts(products);

        ServerLogger.logTransaction(sellerUsername, "ADD_PRODUCT",
                "productId=" + p.getProductId() + ", name=" + p.getName());

        return XMLParser.createResponse("SUCCESS",
                "Product added: " + p.getName() + " [" + p.getProductId() + "]");
    }


    public String fetchAllProducts(String username) {
        List<Product> all = XMLReader.readProducts();

        StringBuilder data = new StringBuilder("<products>");
        for (Product p : all) {
            if ("AVAILABLE".equals(p.getStatus()) && p.getAvailableQuantity() > 0) {
                data.append(productToXML(p));
            }
        }
        data.append("</products>");

        ServerLogger.logTransaction(username, "FETCH_ALL_PRODUCTS", "N/A");
        return XMLParser.createResponseWithData("SUCCESS", "Products fetched.", data.toString());
    }


    public String fetchSellerProducts(String sellerUsername) {
        List<Product> all = XMLReader.readProducts();

        StringBuilder data = new StringBuilder("<products>");
        for (Product p : all) {
            if (sellerUsername.equals(p.getSellerUsername())) {
                data.append(productToXML(p));
            }
        }
        data.append("</products>");

        ServerLogger.logTransaction(sellerUsername, "FETCH_SELLER_PRODUCTS", "N/A");
        return XMLParser.createResponseWithData("SUCCESS", "Your products fetched.", data.toString());
    }


    public String updateProduct(String sellerUsername, Element dataElement) {
        String productId = XMLParser.getTagValue(dataElement, "productId");

        List<Product> products = XMLReader.readProducts();
        for (Product p : products) {
            if (p.getProductId().equals(productId)
                    && p.getSellerUsername().equals(sellerUsername)) {

                // Update only fields provided
                setIfNotNull(p, dataElement);
                XMLWriter.writeProducts(products);

                ServerLogger.logTransaction(sellerUsername, "UPDATE_PRODUCT",
                        "productId=" + productId);
                return XMLParser.createResponse("SUCCESS", "Product updated.");
            }
        }
        return XMLParser.createResponse("FAILED",
                "Product not found or you don't own it.");
    }


    public String deleteProduct(String sellerUsername, Element dataElement) {
        String productId = XMLParser.getTagValue(dataElement, "productId");

        List<Product> products = XMLReader.readProducts();
        boolean removed = products.removeIf(p ->
                p.getProductId().equals(productId)
                        && p.getSellerUsername().equals(sellerUsername));

        if (removed) {
            XMLWriter.writeProducts(products);
            ServerLogger.logTransaction(sellerUsername, "DELETE_PRODUCT",
                    "productId=" + productId);
            return XMLParser.createResponse("SUCCESS", "Product deleted.");
        }
        return XMLParser.createResponse("FAILED",
                "Product not found or you don't own it.");
    }


    public String searchProducts(String username, Element dataElement) {
        String keyword = XMLParser.getTagValue(dataElement, "keyword");
        if (keyword == null) keyword = "";
        String kw = keyword.toLowerCase();

        List<Product> all = XMLReader.readProducts();

        StringBuilder data = new StringBuilder("<products>");
        for (Product p : all) {
            if ("AVAILABLE".equals(p.getStatus()) && p.getAvailableQuantity() > 0) {
                if (p.getName().toLowerCase().contains(kw)
                        || p.getCategory().toLowerCase().contains(kw)) {
                    data.append(productToXML(p));
                }
            }
        }
        data.append("</products>");

        ServerLogger.logTransaction(username, "SEARCH_PRODUCTS", "keyword=" + keyword);
        return XMLParser.createResponseWithData("SUCCESS", "Search results.", data.toString());
    }


    public String buyProduct(String buyerUsername, Element dataElement) {
        String productId = XMLParser.getTagValue(dataElement, "productId");
        int    requested = toInt(XMLParser.getTagValue(dataElement, "quantity"));
        if (requested <= 0) requested = 1;


        synchronized (purchaseLock) {
            List<Product> products = XMLReader.readProducts();
            Product target = null;
            for (Product p : products) {
                if (p.getProductId().equals(productId)) {
                    target = p;
                    break;
                }
            }

            if (target == null) {
                return XMLParser.createResponse("FAILED", "Product not found.");
            }
            if (!"AVAILABLE".equals(target.getStatus())) {
                return XMLParser.createResponse("FAILED", "Product is no longer available.");
            }
            if (target.getAvailableQuantity() < requested) {
                return XMLParser.createResponse("FAILED",
                        "Not enough stock. Available: " + target.getAvailableQuantity());
            }

            // Deduct stock
            target.setAvailableQuantity(target.getAvailableQuantity() - requested);
            if (target.getAvailableQuantity() == 0) {
                target.setStatus("UNAVAILABLE");
            }
            XMLWriter.writeProducts(products);

            // Record transaction
            Transaction tx = new Transaction(
                    "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    productId,
                    buyerUsername,
                    target.getSellerUsername(),
                    requested,
                    LocalDateTime.now().format(TS_FORMAT)
            );
            XMLWriter.appendTransaction(tx);

            ServerLogger.logTransaction(buyerUsername, "BUY_PRODUCT",
                    "productId=" + productId + ", qty=" + requested
                            + ", seller=" + target.getSellerUsername());

            return XMLParser.createResponse("SUCCESS",
                    "Purchase successful! Transaction ID: " + tx.getTransactionId());
        }
    }


    private String productToXML(Product p) {
        return "<product>"
                + "<productId>" + p.getProductId() + "</productId>"
                + "<sellerUsername>" + p.getSellerUsername() + "</sellerUsername>"
                + "<name>" + p.getName() + "</name>"
                + "<category>" + p.getCategory() + "</category>"
                + "<originalPrice>" + p.getOriginalPrice() + "</originalPrice>"
                + "<discountedPrice>" + p.getDiscountedPrice() + "</discountedPrice>"
                + "<availableQuantity>" + p.getAvailableQuantity() + "</availableQuantity>"
                + "<expiryDate>" + p.getExpiryDate() + "</expiryDate>"
                + "<status>" + p.getStatus() + "</status>"
                + "</product>";
    }

    private void setIfNotNull(Product p, Element e) {
        String name     = XMLParser.getTagValue(e, "name");
        String category = XMLParser.getTagValue(e, "category");
        String origP    = XMLParser.getTagValue(e, "originalPrice");
        String discP    = XMLParser.getTagValue(e, "discountedPrice");
        String qty      = XMLParser.getTagValue(e, "availableQuantity");
        String expiry   = XMLParser.getTagValue(e, "expiryDate");
        String status   = XMLParser.getTagValue(e, "status");

        if (name     != null && !name.isEmpty())     p.setName(name);
        if (category != null && !category.isEmpty()) p.setCategory(category);
        if (origP    != null && !origP.isEmpty())    p.setOriginalPrice(toDouble(origP));
        if (discP    != null && !discP.isEmpty())    p.setDiscountedPrice(toDouble(discP));
        if (qty      != null && !qty.isEmpty())      p.setAvailableQuantity(toInt(qty));
        if (expiry   != null && !expiry.isEmpty())   p.setExpiryDate(expiry);
        if (status   != null && !status.isEmpty())   p.setStatus(status);
    }

    private double toDouble(String val) {
        try { return val != null ? Double.parseDouble(val) : 0.0; }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int toInt(String val) {
        try { return val != null ? Integer.parseInt(val) : 0; }
        catch (NumberFormatException e) { return 0; }
    }
}