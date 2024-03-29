package main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    @RequestMapping("/")
    public String defaultController() {
        return "index";
    }

    @RequestMapping("/**/{path:[^\\\\.]*}")
    public String goForward() {
        return "forward:/";
    }
}
