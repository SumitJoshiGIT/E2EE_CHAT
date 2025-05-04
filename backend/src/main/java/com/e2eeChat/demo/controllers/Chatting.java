package com.e2eeChat.demo.controllers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Chatting {
    @GetMapping("/")
    public String index(){
       return "Hello World"; 
    }
     
}


