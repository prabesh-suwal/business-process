package com.cas.server.domain.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category;

    @ManyToMany
    @JoinTable(name = "product_permissions", joinColumns = @JoinColumn(name = "permission_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private Set<Product> products = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Returns the permission code.
     */
    public String getFullCode() {
        return code;
    }
}
