package com.nthienan.ci.sample;

import com.nthienan.ci.sample.model.Post;
import com.nthienan.ci.sample.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
@RestController
public class Application {

    @Autowired
    private PostRepository postRepo;

    @RequestMapping("/")
    public String home() {
        return "Scenario 1: Legacy applications that don’t run on k8s";
    }

    @RequestMapping("/posts")
    public List<Post> getAllPosts() {
        return  postRepo.getAll();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
