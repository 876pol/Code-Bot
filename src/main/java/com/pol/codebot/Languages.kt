package com.pol.codebot

enum class Languages(
    val langName: String,
    val fileExtension: String,
    val program: String,
    val imagePath: String,
    val isCompiled: Boolean,
) {
    C(
        "C",
        ".c",
        "gcc",
        "logos/c.png",
        true
    ),
    CPP("C++",
        ".cpp",
        "g++",
        "logos/cpp.png",
        true
    ),
    JAVA(
        "Java",
        ".java",
        "java",
        "logos/java.png",
        false
    ),
    PYTHON2(
        "Python",
        ".py",
        "python2",
        "logos/python.png",
        false
    ),
    PYTHON3(
        "Python",
        ".py",
        "python3",
        "logos/python.png",
        false
    ),
    SHELL("Shell",
        ".sh",
        "bash",
        "logos/shell.png",
        false
    ),
    HTML("HTML",
        ".html",
        "",
        "logos/html.png",
        false
    ),
    JS(
        "Javascript",
        ".js",
        "node",
        "logos/javascript.png",
        false
    );

}