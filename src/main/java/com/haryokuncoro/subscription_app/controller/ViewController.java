package com.haryokuncoro.subscription_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "users/index";
    }

    @GetMapping("/users")
    public String user() {
        return "users/list";
    }

    @GetMapping("/plans")
    public String plan() {
        return "plans/list";
    }

    @GetMapping("/subscriptions")
    public String subscription() {
        return "subscriptions/list";
    }

    @GetMapping("/invoices")
    public String invoice() {
        return "invoices/list";
    }

}
