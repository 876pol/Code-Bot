package com.pol.codebot

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.logging.Level
import java.util.logging.Logger

enum class Languages(
    val langName: String,
    val fileExtension: String,
    val imagePath: String,
    val compileAndReturnCommandAndSendErrorMessage: (File) -> Pair<ProcessBuilder?, String>
) {
    C(
        "C",
        ".c",
        "images/logos/c.png",
        { codeFile: File ->
            val runCommandBuilder = ProcessBuilder(
                "g++", codeFile.absolutePath, "-o",
                codeFile.name + ".out"
            )
            runCommandBuilder.directory(codeFile.parentFile)
            runCommandBuilder.redirectErrorStream(true)
            val runningCodeProcess = runCommandBuilder.start()
            val codeBufferedReader = BufferedReader(
                InputStreamReader(runningCodeProcess.inputStream)
            )
            var line: Int
            val output = StringBuilder()
            while (codeBufferedReader.read().also { line = it } != -1) {
                output.append(line.toChar())
            }
            runningCodeProcess.waitFor()
            Pair(
                if (runningCodeProcess.exitValue() == 0) {
                    if (Main.os == "Windows") {
                        ProcessBuilder("cmd", "/C", codeFile.name + ".out")
                    } else {
                        setWritableParentFolderAndLog(codeFile)
                        ProcessBuilder("bash", "-c", "ulimit -Sv 64000000 && ./" + codeFile.name + ".out")
                    }
                } else null, output.toString()
            )
        }
    ),
    CPP("C++",
        ".cpp",
        "images/logos/cpp.png",
        { codeFile: File ->
            val runCommandBuilder = ProcessBuilder(
                "gcc", codeFile.absolutePath, "-o",
                codeFile.name + ".out"
            )
            runCommandBuilder.directory(codeFile.parentFile)
            runCommandBuilder.redirectErrorStream(true)
            val runningCodeProcess = runCommandBuilder.start()
            val codeBufferedReader = BufferedReader(
                InputStreamReader(runningCodeProcess.inputStream)
            )
            var line: Int
            val output = StringBuilder()
            while (codeBufferedReader.read().also { line = it } != -1) {
                output.append(line.toChar())
            }
            runningCodeProcess.waitFor()
            Pair(
                if (runningCodeProcess.exitValue() == 0) {
                    if (Main.os == "Windows") {
                        ProcessBuilder("cmd", "/C", codeFile.name + ".out")
                    } else {
                        setWritableParentFolderAndLog(codeFile)
                        ProcessBuilder("bash", "-c", "ulimit -Sv 64000000 && ./" + codeFile.name + ".out")
                    }
                } else null, output.toString()
            )
        }
    ),
    JAVA(
        "Java",
        ".java",
        "images/logos/java.png",
        { codeFile: File ->
            if (Thread.currentThread().stackTrace[3].className == Programming::class.java.name) {
                Pair(
                    if (Main.os == "Windows") {
                        ProcessBuilder(
                            "cmd", "/C",
                            "java " + codeFile.absolutePath
                        )
                    } else {
                        setWritableParentFolderAndLog(codeFile)
                        ProcessBuilder(
                            "bash", "-c",
                            "ulimit -Sv 64000000 && java " + codeFile.absolutePath
                        )
                    }, ""
                )
            } else {
                val runCommandBuilder = ProcessBuilder(
                    "javac", codeFile.absolutePath
                )
                runCommandBuilder.directory(codeFile.parentFile)
                runCommandBuilder.redirectErrorStream(true)
                val runningCodeProcess = runCommandBuilder.start()
                val codeBufferedReader = BufferedReader(
                    InputStreamReader(runningCodeProcess.inputStream)
                )
                var line: Int
                val output = StringBuilder()
                while (codeBufferedReader.read().also { line = it } != -1) {
                    output.append(line.toChar())
                }
                runningCodeProcess.waitFor()
                Pair(
                    if (runningCodeProcess.exitValue() == 0) {
                        if (Main.os == "Windows") {
                            ProcessBuilder("cmd", "/C", "java " + codeFile.nameWithoutExtension)
                        } else {
                            setWritableParentFolderAndLog(codeFile)
                            ProcessBuilder(
                                "bash",
                                "-c",
                                "ulimit -Sv 64000000 && java " + codeFile.nameWithoutExtension
                            )
                        }
                    } else null, output.toString()
                )
            }
        }
    ),
    PYTHON2(
        "Python",
        ".py",
        "images/logos/python.png",
        { codeFile: File ->
            Pair(
                if (Main.os == "Windows") {
                    ProcessBuilder(
                        "cmd", "/C",
                        "python2 " + codeFile.absolutePath
                    )
                } else {
                    setWritableParentFolderAndLog(codeFile)
                    ProcessBuilder(
                        "bash", "-c",
                        "ulimit -Sv 64000000 && python2 " + codeFile.absolutePath
                    )
                }, ""
            )
        }
    ),
    PYTHON3(
        "Python",
        ".py",
        "images/logos/python.png",
        { codeFile: File ->
            Pair(
                if (Main.os == "Windows") {
                    ProcessBuilder(
                        "cmd", "/C",
                        "python3 " + codeFile.absolutePath
                    )
                } else {
                    setWritableParentFolderAndLog(codeFile)
                    ProcessBuilder(
                        "bash", "-c",
                        "ulimit -Sv 64000000 && python3 " + codeFile.absolutePath
                    )
                }, ""
            )
        }
    ),
    SHELL("Shell",
        ".sh",
        "images/logos/shell.png",
        { codeFile: File ->
            setWritableParentFolderAndLog(codeFile)
            Pair(
                ProcessBuilder(
                    "bash", "-c",
                    "ulimit -Sv 64000000 && ./" + codeFile.absolutePath
                ), ""
            )
        }
    ),
    HTML("HTML",
        ".html",
        "images/logos/html.png",
        { Pair(null, "") }
    ),
    JS(
        "Javascript",
        ".js",
        "images/logos/javascript.png",
        { codeFile: File ->
            setWritableParentFolderAndLog(codeFile)
            Pair(
                if (Main.os == "Windows") {
                    ProcessBuilder(
                        "cmd", "/C",
                        "node " + codeFile.absolutePath
                    )
                } else {
                    ProcessBuilder(
                        "bash", "-c",
                        "ulimit -Sv 64000000 && node " + codeFile.absolutePath
                    )
                }, ""
            )
        }
    );
}

fun setWritableParentFolderAndLog(file: File) {
    if (!file.parentFile.setWritable(false)) {
        Logger.getLogger(Languages::class.java.name)
            .log(Level.WARNING, "Folder still writable: " + file.parentFile)
    }
}