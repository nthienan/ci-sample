package com.tma.ci4tma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    @RequestMapping("/")
    public String home() {
        return "Hello Team ! How are you?";
    }

    @RequestMapping("/{name}")
    public String hello(@PathVariable String name) {
        return "Hello " + name;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
