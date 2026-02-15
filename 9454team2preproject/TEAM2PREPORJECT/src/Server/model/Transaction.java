package Server.model;

public class Transaction {
    private String transactionId;
    private String productId;
    private String buyerUsername;
    private String sellerUsername;
    private int quantity;
    private String timestamp; // Format: YYYY-MM-DDTHH:MM:SS

    public Transaction() {
    }

    public Transaction(String transactionId, String productId, String buyerUsername,
                       String sellerUsername, int quantity, String timestamp) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // Getters and Setters...
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public void setBuyerUsername(String buyerUsername) {
        this.buyerUsername = buyerUsername;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", productId='" + productId + '\'' +
                ", buyerUsername='" + buyerUsername + '\'' +
                ", sellerUsername='" + sellerUsername + '\'' +
                ", quantity=" + quantity +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}