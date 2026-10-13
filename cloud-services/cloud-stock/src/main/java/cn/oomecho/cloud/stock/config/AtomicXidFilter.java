package cn.oomecho.cloud.stock.config;

import cn.oomecho.cloud.common.core.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.core.context.RootContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AT 专用 HTTP 入口的 XID 生命周期；仅绑定指定内部写路径，finally 清理线程防止串单
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Component
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class AtomicXidFilter extends OncePerRequestFilter {
    private final ObjectMapper json;

    /**
     * @param json 统一响应序列化
     */
    public AtomicXidFilter(ObjectMapper json) {
        this.json = json;
    }

    /**
     * @param request 当前请求 @return 其他路径不接受外部 XID 参与全局事务
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/internal/stocks/atomic-reservations".equals(request.getRequestURI());
    }

    /**
     * @param request HTTP 请求 @param response HTTP 响应 @param chain 后续处理链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String xid = request.getHeader(RootContext.KEY_XID);
        if (xid == null || xid.isBlank() || xid.length() > 128 || xid.chars().anyMatch(Character::isWhitespace)) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(), new Result<>(40002, "AT 扣减必须携带有效全局事务 XID", null, System.currentTimeMillis()));
            return;
        }
        String previous = RootContext.getXID();
        boolean bound = previous == null;
        if (previous != null && !previous.equals(xid)) {
            response.setStatus(409);
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(), Result.failure(40909, "当前全局事务上下文与请求不一致"));
            return;
        }
        if (bound) RootContext.bind(xid);
        try {
            chain.doFilter(request, response);
        } finally {
            if (bound) RootContext.unbind();
        }
    }
}
