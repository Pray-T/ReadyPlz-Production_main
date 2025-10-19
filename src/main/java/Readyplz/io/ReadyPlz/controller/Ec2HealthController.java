package Readyplz.io.ReadyPlz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;



@RestController
public class Ec2HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> healthChekck() {
        return ResponseEntity.ok("Yes, It's Healthy");
    }

}
