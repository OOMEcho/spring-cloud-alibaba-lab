package cn.oomecho.cloud.product.service;


import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.product.domain.dto.ProductUpdateDTO;
import cn.oomecho.cloud.product.domain.entity.Product;
import org.mapstruct.*;

/**
 * 商品 DTO、Entity 和跨服务响应的 MapStruct 映射
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductConvert {
    /**
     * @param product 数据库商品
     * @return 商品响应
     */
    ProductResponse toResponse(Product product);

    /**
     * @param dto     更新参数
     * @param product 目标实体，保留 ID
     */
    @Mapping(target = "id", ignore = true)
    void update(ProductUpdateDTO dto, @MappingTarget Product product);
}
