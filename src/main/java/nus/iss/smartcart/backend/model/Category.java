package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "category")
public class Category {

    public Category() {
        // Required by JPA - Hibernate instantiates entities via reflection when loading from the DB.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    //Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
