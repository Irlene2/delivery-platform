package com.deliveryplatform.catalog.infrastructure.persistence.repository;

import com.deliveryplatform.catalog.infrastructure.persistence.entity.ImagemDocumento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ImagemSpringDataRepository extends MongoRepository<ImagemDocumento, UUID> {
}
