package com.position_service.service;

import com.position_service.model.OrderEvent;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PositionService {

   private final  Map<String , Long> map = new ConcurrentHashMap<>();
   private  final Set<String> set = ConcurrentHashMap.newKeySet();

    public boolean recordEvent(OrderEvent event) {
        if (set.contains(event.getEventId())) {
            System.out.println("INFO: duplicate event ignored -> " + event.getEventId());
            return false;
        }
        set.add(event.getEventId());

        long change = "BUY".equals(event.getTransactionType()) ? event.getQuantity() : -event.getQuantity();
        map.merge(event.getSymbol(), change, Long::sum);

        System.out.println("INFO: recorded " + event.getEventId() + " -> " + event.getSymbol());
        return true;
    }

    public Map<String, Long> getPositions() {
        return map;
    }
}
