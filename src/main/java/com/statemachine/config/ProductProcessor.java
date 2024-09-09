package com.statemachine.config;

import com.statemachine.model.Product;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import javax.annotation.processing.Processor;
@Component
public class ProductProcessor implements ItemProcessor<Product , Product> {
    @Override
    public Product process(Product item) throws Exception {
        if (item.getPrice() < 0) {
            throw new DataIntegrityViolationException("Negative price encountered for product: " + item.getName());
        }
        return item;
    }
}
