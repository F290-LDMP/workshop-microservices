package br.com.fatecararas.microservices.core.product.persistence;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<ProductEntity, String> {
    Optional<ProductEntity> findByProductId(int productId);
}
