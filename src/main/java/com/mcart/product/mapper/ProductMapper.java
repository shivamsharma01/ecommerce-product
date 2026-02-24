package com.mcart.product.mapper;

import com.mcart.product.dto.ProductResponse;
import com.mcart.product.model.ProductDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "productId", target = "id")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "dateToInstant")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "dateToInstant")
    ProductResponse toResponse(ProductDocument product);

    @Named("dateToInstant")
    default Instant dateToInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
