package com.pol.codebot;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
@RestController
public class Controller implements ErrorController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public String pong() {
        return "pong";
    }

    @RequestMapping(value = "/error")
    @ResponseBody
    public String error() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream attachment = classloader.getResourceAsStream("error/index.html");
        assert attachment != null;
        return new String(attachment.readAllBytes(), StandardCharsets.UTF_8);
    }

    @RequestMapping(value = "/help")
    @ResponseBody
    public String help() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream attachment = classloader.getResourceAsStream("help/index.html");
        assert attachment != null;
        return new String(attachment.readAllBytes(), StandardCharsets.UTF_8);
    }

    @GetMapping("/html/{urlParameter}")
    public String htmlCode(@PathVariable("urlParameter") String urlParameter) throws IOException {
        String page = HTML.fileMap.get(urlParameter);
        return page == null ? error() : page;
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}