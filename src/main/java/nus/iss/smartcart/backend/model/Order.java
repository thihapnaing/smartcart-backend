package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "orders")
public class Order {

    public Order() {
        // Required by JPA - Hibernate instantiates entities via reflection when loading from the DB.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    private String firstName;

    private String lastName;

    private String shippingAddress;

    private String phoneNumber;

    private LocalDateTime deliveredAt;

    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
    }

    @Column(name = "tracking_no", unique = true)
    private String trackingNo;

    @Column(name = "delivery_person_id")
    private Long deliveryPersonId;

    @Column(name = "delivery_proof_key")
    private String deliveryProofKey;

    //Getters and Setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // for delivery app
    public String getTrackingNo() {return trackingNo;}

    public void setTrackingNo(String trackingNo) {this.trackingNo = trackingNo;}

    public Long getDeliveryPersonId() {return deliveryPersonId;}

    public void setDeliveryPersonId(Long deliveryPersonId) {this.deliveryPersonId = deliveryPersonId;}

    public String getDeliveryProofKey() {return deliveryProofKey;}

    public void setDeliveryProofKey(String deliveryProofKey) {this.deliveryProofKey = deliveryProofKey;}
}
