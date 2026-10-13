package cn.oomecho.cloud.order.controller;


import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.config.LearningProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习配置查询接口；观察配置刷新后的实际取值
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@RequestMapping(value = "/learning", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "配置验证接口")
public class LearningController {
    private final LearningProperties properties;

    /**
     * @param properties 可刷新业务配置
     */
    public LearningController(LearningProperties properties) {
        this.properties = properties;
    }

    /**
     * @return 当前文案与购买数量限制，用于确认 Nacos 配置刷新
     */
    @GetMapping("/config")
    @Operation(summary = "查询当前生效的业务配置")
    public Result<ConfigResponse> config() {
        return Result.success(new ConfigResponse(properties.getMessage(), properties.getMaxQuantity()));
    }

    /**
     * 配置快照，只公开实验业务配置。
     *
     * @param message     当前文案
     * @param maxQuantity 当前购买数量上限
     * @author xuesong.lei
     * @since 2026-10-12
     */
    public record ConfigResponse(String message, int maxQuantity) {
    }
}
