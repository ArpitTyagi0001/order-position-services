package com.order_update_service.config;

import com.order_update_service.model.OrderEvent;
import com.order_update_service.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;

@Service
public class CsvProcessor {
    private final OrderService orderService;
    private final RestTemplate restTemplate = new RestTemplate();

    public CsvProcessor(OrderService orderService) {
        this.orderService = orderService;
    }

    @Value("${order.csv.path}")
    private String csvPath;

    @Value("${position.service.url}")
    private String positionServiceUrl;

    public void process() throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(csvPath));

        String line = reader.readLine();
        int sentCount = 0;

        while((line = reader.readLine()) != null){
            String[] parts = line.split("," , -1);
            if(parts.length != 4){
                System.out.println("WARN: bad row -> " + line);
                continue;
            }

            String eventId = parts[0].trim();
            String symbol = parts[1].trim();
            String type = parts[2].trim();
            Long quantity = null;

            try{
                if(!parts[3].trim().isEmpty()){
                    quantity = Long.parseLong(parts[3].trim());
                }
            }catch (NumberFormatException e){
                System.out.println("WARN: invalid quantity -> " + line);
                continue;
            }

            OrderEvent event = new OrderEvent(eventId , symbol , type , quantity);

            if(!orderService.validateOrder(event)){
               continue;
            }

            try{
                restTemplate.postForEntity(positionServiceUrl + "/events" , event , Void.class);
                sentCount++;
                System.out.println("INFO: sent event " + eventId);
            } catch (Exception e) {
                System.out.println("ERROR: failed to send " + eventId + " -> " + e.getMessage());
            }

            Thread.sleep(50);


        }
        reader.close();
        System.out.println();
        System.out.println("INFO: processing complete, total sent: " + sentCount);
    }

}
