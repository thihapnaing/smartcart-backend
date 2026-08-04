package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "category")
public class Category {

    public Category() {}

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
