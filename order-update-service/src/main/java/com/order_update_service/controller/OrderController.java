package com.order_update_service.controller;

import com.order_update_service.model.OrderEvent;
import com.order_update_service.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orderCsv")
    public void OrderCsv(@RequestBody OrderEvent orderEvent){
        orderService.validateOrder(orderEvent);
    }
}
