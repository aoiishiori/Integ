package Server.util;

import Server.model.Product;
import Server.model.Account;
import Server.model.Transaction;
import java.io.*;
import java.util.*;

public class XMLWriter {

    private static final String DATA_DIR = "data/";
    private static final String ACCOUNTS_FILE = DATA_DIR + "Accounts.xml";
    private static final String PRODUCTS_FILE = DATA_DIR + "Products.xml";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "Transactions.xml";

    private static final Object accountsLock = new Object();
    private static final Object productsLock = new Object();
    private static final Object transactionsLock = new Object();

    public static void initializeDataFiles() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        createIfAbsent(ACCOUNTS_FILE,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Accounts>\n</Accounts>");

        createIfAbsent(PRODUCTS_FILE,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Products>\n</Products>");

        createIfAbsent(TRANSACTIONS_FILE,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Transactions>\n</Transactions>");

        // Also initialize the log file
        ServerLogger.initLogFile();
    }

    private static void createIfAbsent(String path, String defaultContent) {
        File file = new File(path);
        if (!file.exists()) {
            writeToFile(path, defaultContent);
        }
    }

    public static void writeAccounts(List<Account> accounts) {
        synchronized (accountsLock) {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<Accounts>\n");

            for (Account account : accounts) {
                xml.append("  <Account>\n");
                xml.append("    <accountId>").append(esc(account.getAccountId())).append("</accountId>\n");
                xml.append("    <username>").append(esc(account.getUsername())).append("</username>\n");
                xml.append("    <password>").append(esc(account.getPassword())).append("</password>\n");
                xml.append("    <role>").append(esc(account.getRole())).append("</role>\n");
                xml.append("    <status>").append(esc(account.getStatus())).append("</status>\n");
                xml.append("  </Account>\n");
            }

            xml.append("</Accounts>");
            writeToFile(ACCOUNTS_FILE, xml.toString());
        }
    }

    public static void writeProducts(List<Product> products) {
        synchronized (productsLock) {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<Products>\n");

            for (Product product : products) {
                xml.append("  <Product>\n");
                xml.append("    <productId>").append(esc(product.getProductId())).append("</productId>\n");
                xml.append("    <sellerUsername>").append(esc(product.getSellerUsername())).append("</sellerUsername>\n");
                xml.append("    <name>").append(esc(product.getName())).append("</name>\n");
                xml.append("    <category>").append(esc(product.getCategory())).append("</category>\n");
                xml.append("    <originalPrice>").append(product.getOriginalPrice()).append("</originalPrice>\n");
                xml.append("    <discountedPrice>").append(product.getDiscountedPrice()).append("</discountedPrice>\n");
                xml.append("    <availableQuantity>").append(product.getAvailableQuantity()).append("</availableQuantity>\n");
                xml.append("    <expiryDate>").append(esc(product.getExpiryDate())).append("</expiryDate>\n");
                xml.append("    <status>").append(esc(product.getStatus())).append("</status>\n");
                xml.append("  </Product>\n");
            }

            xml.append("</Products>");
            writeToFile(PRODUCTS_FILE, xml.toString());
        }
    }

    public static void writeTransactions(List<Transaction> transactions) {
        synchronized (transactionsLock) {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<Transactions>\n");

            for (Transaction transaction : transactions) {
                xml.append("  <Transaction>\n");
                xml.append("    <transactionId>").append(esc(transaction.getTransactionId())).append("</transactionId>\n");
                xml.append("    <productId>").append(esc(transaction.getProductId())).append("</productId>\n");
                xml.append("    <buyerUsername>").append(esc(transaction.getBuyerUsername())).append("</buyerUsername>\n");
                xml.append("    <sellerUsername>").append(esc(transaction.getSellerUsername())).append("</sellerUsername>\n");
                xml.append("    <quantity>").append(transaction.getQuantity()).append("</quantity>\n");
                xml.append("    <timestamp>").append(esc(transaction.getTimestamp())).append("</timestamp>\n");
                xml.append("  </Transaction>\n");
            }

            xml.append("</Transactions>");
            writeToFile(TRANSACTIONS_FILE, xml.toString());
        }
    }

    public static void appendTransaction(Transaction t) {
        synchronized (transactionsLock) {
            File file = new File(TRANSACTIONS_FILE);
            if (!file.exists()) {
                writeToFile(TRANSACTIONS_FILE,
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Transactions>\n</Transactions>");
            }

            try {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }

                String entry = "  <Transaction>\n"
                        + "    <transactionId>" + esc(t.getTransactionId()) + "</transactionId>\n"
                        + "    <productId>" + esc(t.getProductId()) + "</productId>\n"
                        + "    <buyerUsername>" + esc(t.getBuyerUsername()) + "</buyerUsername>\n"
                        + "    <sellerUsername>" + esc(t.getSellerUsername()) + "</sellerUsername>\n"
                        + "    <quantity>" + t.getQuantity() + "</quantity>\n"
                        + "    <timestamp>" + esc(t.getTimestamp()) + "</timestamp>\n"
                        + "  </Transaction>\n";

                String updated = content.toString()
                        .replace("</Transactions>", entry + "</Transactions>");
                writeToFile(TRANSACTIONS_FILE, updated);

            } catch (IOException e) {
                System.err.println("[ERROR] appendTransaction: " + e.getMessage());
            }
        }
    }

    private static void writeToFile(String filename, String content) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write to file " + filename + ": " + e.getMessage());
        }
    }

    private static String esc(String text) { // escapeXML renamed to esc for method usage
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}

 // for initializeDataFiles
/* refactored
        // Creates Accounts.xml if it does not exist yet...
        File accountsFile = new File(ACCOUNTS_FILE);
        if (!accountsFile.exists()) {
            writeToFile(ACCOUNTS_FILE, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Accounts>\n</Accounts>");
        }

        // Creates Products.xml if it does not exist yet...
        File productsFile = new File(PRODUCTS_FILE);
        if (!productsFile.exists()) {
            writeToFile(PRODUCTS_FILE, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Products>\n</Products>");
        }

        // Creates Transactions.xml if it does not exist yet...
        File transactionsFile = new File(TRANSACTIONS_FILE);
        if (!transactionsFile.exists()) {
            writeToFile(TRANSACTIONS_FILE, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Transactions>\n</Transactions>");
        }
    }

         */