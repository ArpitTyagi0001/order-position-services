package com.order_update_service.service;

import com.order_update_service.model.OrderEvent;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;


@Service
public class OrderService {

    Set<String> set = new HashSet<>();

    public boolean validateOrder(OrderEvent event) {
        if(event.getEventId() == null || event.getEventId().isBlank()){
            System.out.println("WARN : eventId can't be Null or Blank");
            return false;
        }


        if(event.getSymbol() == null || event.getSymbol().isBlank()){
            System.out.println("WARN : symbol can't be Null or Blank");
            return false;
        }


        if(!"BUY".equals(event.getTransactionType()) && !"SELL".equals(event.getTransactionType())){
            System.out.println("WARN : transactionType must be BUY or SELL only");
            return false;
        }

        if(event.getQuantity()==null || event.getQuantity()<=0){
            System.out.println("WARN : quantity only be positive");
            return false;
        }

        if(set.contains(event.getEventId())){
            System.out.println("Duplicate eventId");
            return false;
        }

        set.add(event.getEventId());
        return true;
    }
}
