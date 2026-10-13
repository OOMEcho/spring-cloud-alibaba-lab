package cn.oomecho.cloud.product.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品展示缓存；代际校验避免更新后被并发旧查询重新填入旧数据
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class ProductCache {
    private static final Logger log = LoggerFactory.getLogger(ProductCache.class);
    /**
     * 写缓存前检查读取时的代际；商品已更新则拒绝旧查询回填。
     */
    private static final DefaultRedisScript<Long> PUT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1]) or '0'
            if current ~= ARGV[1] then return 0 end
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
            return 1
            """, Long.class);
    /**
     * 提升代际并删除旧代缓存，操作在 Redis 中原子执行。
     */
    private static final DefaultRedisScript<Long> INVALIDATE = new DefaultRedisScript<>("""
            local old = redis.call('GET', KEYS[1]) or '0'
            redis.call('INCR', KEYS[1])
            redis.call('DEL', KEYS[2] .. old)
            return 1
            """, Long.class);
    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final String prefix;
    private final long ttl;

    /**
     * @param redis   Redis 客户端
     * @param enabled 是否启用缓存
     * @param prefix  本项目键前缀
     * @param ttl     展示数据有效秒数
     */
    public ProductCache(StringRedisTemplate redis, @Value("${lab.cache.enabled:true}") boolean enabled,
                        @Value("${lab.cache.prefix:cloud-lab:}") String prefix,
                        @Value("${lab.cache.ttl-seconds:30}") long ttl) {
        if (ttl < 1) {
            throw new IllegalArgumentException("缓存 TTL 必须为正数");
        }
        this.redis = redis;
        this.enabled = enabled;
        this.prefix = prefix;
        this.ttl = ttl;
    }

    /**
     * @param id 商品 ID
     * @return 缓存值与读取时的代际，Redis 故障时标记不可写回
     */
    public Snapshot read(Long id) {
        if (!enabled) {
            return new Snapshot("0", null, false);
        }
        try {
            String generation = redis.opsForValue().get(generationKey(id));
            if (generation == null) {
                generation = "0";
            }
            return new Snapshot(generation, redis.opsForValue().get(dataBase(id) + generation), true);
        } catch (DataAccessException exception) {
            log.warn("商品缓存读取失败，回退数据库，productId={}", id);
            return new Snapshot("0", null, false);
        }
    }

    /**
     * @param id       商品 ID
     * @param snapshot 查询前的代际
     * @param json     数据库查询结果 JSON
     */
    public void put(Long id, Snapshot snapshot, String json) {
        if (!snapshot.redisAvailable()) {
            return;
        }
        try {
            redis.execute(PUT, List.of(generationKey(id), dataBase(id) + snapshot.generation()),
                    snapshot.generation(), json, String.valueOf(ttl));
        } catch (DataAccessException exception) {
            log.warn("商品缓存写入失败，数据库结果仍可返回，productId={}", id);
        }
    }

    /**
     * @param id 已提交数据库更新的商品 ID
     */
    public void invalidate(Long id) {
        if (!enabled) {
            return;
        }
        try {
            redis.execute(INVALIDATE, List.of(generationKey(id), dataBase(id)));
        } catch (DataAccessException exception) {
            // 数据库已经提交，不能用 Redis 错误伪装数据库回滚；展示缓存最多存活 TTL。
            log.warn("商品更新已提交但缓存失效失败，等待 TTL；下单报价仍直接读取数据库，productId={}", id);
        }
    }

    /**
     * @param id 商品 ID
     * @return 代际键，和数据键使用同一个 Redis hash tag
     */
    private String generationKey(Long id) {
        return prefix + "product:{" + id + "}:generation";
    }

    /**
     * @param id 商品 ID
     * @return 数据键前缀
     */
    private String dataBase(Long id) {
        return prefix + "product:{" + id + "}:data:";
    }

    /**
     * 缓存读取快照；redisAvailable 防止读取失败后回填未知代际。
     *
     * @param generation     读取时的代际
     * @param json           展示数据 JSON，未命中时为 null
     * @param redisAvailable 本次读取是否成功
     * @author xuesong.lei
     * @since 2026-10-12
     */
    public record Snapshot(String generation, String json, boolean redisAvailable) {
    }
}
