package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="product")
public class Product {

    public Product() { /* Intentionally left empty */ }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) //added by Junior
    private String name;

    private String description;

    @Column(precision = 10, scale = 2, nullable = false) //updated by Junior
    private BigDecimal price;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    //added by Junior
    @Column(name = "color", nullable = false)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User merchant;

    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status;

    @Column(name = "admin_locked")
    private Boolean adminLocked = false;

    // AUTHOR: Htet Nandar(Grace)
    // Which admin last changed this listing's status (activate/deactivate) and when - so a
    // deactivation isn't an untraceable action once more than one admin account exists. Null
    // until the first admin-driven status change; never set by anything but
    // AdminProductService.updateProductStatus().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_admin_id")
    private User lastModifiedByAdmin;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Singapore"));
    }

}
