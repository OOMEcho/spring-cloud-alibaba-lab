package cn.oomecho.cloud.product.controller;


import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.product.domain.dto.ProductUpdateDTO;
import cn.oomecho.cloud.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 商品接口；展示查询可缓存，下单报价供服务间调用
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "请求已处理，具体业务结果见 data", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "参数或价格格式错误"),
        @ApiResponse(responseCode = "404", description = "商品不存在"),
        @ApiResponse(responseCode = "409", description = "商品版本冲突"),
        @ApiResponse(responseCode = "500", description = "内部服务错误")
})
@Tag(name = "商品接口")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {
    private final ProductService service;

    /**
     * @param service 商品业务层
     */
    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * @param id 商品 ID
     * @return 可缓存的商品展示信息
     */
    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情", description = "初始商品 ID：1001、1002")
    public Result<ProductResponse> detail(@PathVariable @Positive Long id) {
        return Result.success(service.detail(id));
    }

    /**
     * @param id 商品 ID
     * @return 数据库中的当前报价
     */
    @GetMapping("/internal/products/{id}/quote")
    @Operation(summary = "下单报价", description = "直接查询数据库，不经展示缓存；网关不代理 internal 路径")
    public Result<ProductResponse> quote(@PathVariable @Positive Long id) {
        return Result.success(service.quote(id));
    }

    /**
     * @param id  商品 ID
     * @param dto 新商品值与预期版本
     * @return 更新结果
     */
    @PutMapping("/products/{id}")
    @Operation(summary = "修改商品", description = "提交当前 version，数据库提交后切换缓存代际")
    public Result<ProductResponse> update(@PathVariable @Positive Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        return Result.success(service.update(id, dto));
    }
}
