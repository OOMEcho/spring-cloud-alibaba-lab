package cn.oomecho.cloud.product.service;


import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.product.domain.dto.ProductUpdateDTO;

/**
 * 商品业务层；展示缓存与下单数据库报价分开
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface ProductService {
    /**
     * @param id 商品 ID
     * @return 可以来自展示缓存的商品信息
     */
    ProductResponse detail(Long id);

    /**
     * @param id 商品 ID
     * @return 数据库报价与状态
     */
    ProductResponse quote(Long id);

    /**
     * @param id  商品 ID
     * @param dto 更新参数
     * @return 已提交的新商品信息
     */
    ProductResponse update(Long id, ProductUpdateDTO dto);
}
