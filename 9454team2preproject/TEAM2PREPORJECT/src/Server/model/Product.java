package Server.model;

public class Product {
    private String productId;
    private String sellerUsername;
    private String name;
    private String category;
    private double originalPrice;
    private double discountedPrice;
    private int availableQuantity;
    private String expiryDate; // Format: YYYY-MM-DD
    private String status; // AVAILABLE, UNAVAILABLE

    public Product() {
    }

    public Product(String productId, String sellerUsername, String name, String category,
                   double originalPrice, double discountedPrice, int availableQuantity,
                   String expiryDate, String status) {
        this.productId = productId;
        this.sellerUsername = sellerUsername;
        this.name = name;
        this.category = category;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.availableQuantity = availableQuantity;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    // Getters and Setters...
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", sellerUsername='" + sellerUsername + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", discountedPrice=" + discountedPrice +
                ", availableQuantity=" + availableQuantity +
                ", expiryDate='" + expiryDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}