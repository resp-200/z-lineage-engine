package demo.lineage;

import lineage.annotation.AccessMode;
import lineage.annotation.BizOp;
import lineage.annotation.Effect;
import lineage.annotation.EffectType;

/**
 * 示例业务操作，用于演示编译期图谱推理。
 */
public class App {

    /**
     * 创建订单：写订单并发出创建事件。
     */
    @BizOp(id = "order.create", domain = "order", name = "创建订单")
    @Effect(type = EffectType.TOUCH, target = "Order", access = AccessMode.WRITE, key = "orderId")
    @Effect(type = EffectType.EMIT, target = "OrderCreated")
    public void createOrder() {
        // 示例方法。
    }

    /**
     * 支付订单：消费创建事件并写订单。
     */
    @BizOp(id = "order.pay", domain = "order", name = "支付订单")
    @Effect(type = EffectType.CONSUME, target = "OrderCreated")
    @Effect(type = EffectType.TOUCH, target = "Order", access = AccessMode.WRITE, key = "orderId")
    public void payOrder() {
        // 示例方法。
    }

    /**
     * 取消订单：写同一订单资源，与支付互斥。
     */
    @BizOp(id = "order.cancel", domain = "order", name = "取消订单")
    @Effect(type = EffectType.TOUCH, target = "Order", access = AccessMode.WRITE, key = "orderId")
    public void cancelOrder() {
        // 示例方法。
    }
}
