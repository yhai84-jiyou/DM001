package com.helpmanual.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {

    @RequestMapping(value = {
            "/",
            "/admin",
            "/admin/**",
            "/{path:^(?!api|uploads|assets|css|js|img|fonts|favicon\\.ico).*$}",
            "/{path:^(?!api|uploads|assets|css|js|img|fonts|favicon\\.ico).*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
