package com.statemachine.repository;

import com.statemachine.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRrepo extends JpaRepository<Product, Integer> {
}
