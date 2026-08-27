package com.position_service.controller;

import com.position_service.model.OrderEvent;
import com.position_service.service.PositionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("/position")
public class PositionController {
    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> receiveEvent(@RequestBody OrderEvent event){
       positionService.recordEvent(event);
       return ResponseEntity.ok().build();
    }

    @GetMapping("/position")
    public Map<String, Long> getPosition() {
        return positionService.getPositions();
    }
}
