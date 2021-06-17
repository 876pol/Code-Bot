package com.pol.codebot;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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
        InputStream attachment = classloader.getResourceAsStream("static/error/index.html");
        assert attachment != null;
        return new String(attachment.readAllBytes(), StandardCharsets.UTF_8);
    }

    @GetMapping("/html/{urlParameter}")
    public String htmlCode(@PathVariable("urlParameter") String urlParameter) throws IOException {
        File codeFile = HTML.fileMap.get(urlParameter);
        if (codeFile == null) {
            return error();
        }
        BufferedReader br = new BufferedReader(new FileReader(codeFile));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(System.lineSeparator());
        }
        br.close();
        return stringBuilder.toString();
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}