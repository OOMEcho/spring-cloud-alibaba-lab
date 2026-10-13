package cn.oomecho.cloud.product.service.impl;


import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.product.cache.ProductCache;
import cn.oomecho.cloud.product.domain.dto.ProductUpdateDTO;
import cn.oomecho.cloud.product.domain.entity.Product;
import cn.oomecho.cloud.product.mapper.ProductMapper;
import cn.oomecho.cloud.product.service.ProductConvert;
import cn.oomecho.cloud.product.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 商品业务实现层；数据库提交后失效缓存，报价始终由数据库提供
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductMapper mapper;
    private final ProductConvert convert;
    private final ProductCache cache;
    private final ObjectMapper json;

    /**
     * @param mapper  商品 Mapper
     * @param convert 类型映射
     * @param cache   展示缓存
     * @param json    JSON 序列化器
     */
    public ProductServiceImpl(ProductMapper mapper, ProductConvert convert, ProductCache cache, ObjectMapper json) {
        this.mapper = mapper;
        this.convert = convert;
        this.cache = cache;
        this.json = json;
    }

    /**
     * @param id 商品 ID
     * @return 展示商品信息，缓存故障不阻止数据库查询
     */
    @Override
    public ProductResponse detail(Long id) {
        var snapshot = cache.read(id);
        if (snapshot.json() != null) {
            try {
                ProductResponse cached = json.readValue(snapshot.json(), ProductResponse.class);
                // 合法 JSON 的 null 也可能来自脏缓存，按未命中回退数据库。
                if (cached != null && id.equals(cached.id()) && cached.price() != null && cached.version() != null) {
                    log.info("商品缓存命中，productId={}", id);
                    return cached;
                }
            } catch (JsonProcessingException exception) {
                log.warn("商品缓存内容无法解析，回退数据库，productId={}", id);
            }
        }
        ProductResponse response = quote(id);
        try {
            cache.put(id, snapshot, json.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            log.warn("商品缓存序列化失败，productId={}", id);
        }
        return response;
    }

    /**
     * @param id 商品 ID
     * @return 不受展示缓存影响的数据库报价
     */
    @Override
    public ProductResponse quote(Long id) {
        return convert.toResponse(requireProduct(id));
    }

    /**
     * @param id  商品 ID
     * @param dto 更新参数
     * @return 新版本商品
     */
    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateDTO dto) {
        Product product = requireProduct(id);
        convert.update(dto, product);
        if (mapper.updateVersioned(product) != 1) {
            throw new BusinessException(409, 40903, "商品已被其他请求修改，请重新查询后提交");
        }
        // 只有数据库提交成功才切换缓存代际，避免事务回滚时丢失有效缓存。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.invalidate(id);
            }
        });
        return quote(id);
    }

    /**
     * @param id 商品 ID
     * @return 必须存在的数据库商品
     */
    private Product requireProduct(Long id) {
        Product product = mapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, 40402, "商品不存在");
        }
        return product;
    }
}
