package com.pol.codebot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.springframework.util.FileSystemUtils
import java.io.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.IntStream
import kotlin.io.path.createTempDirectory

class Judge(event: MessageReceivedEvent, code: String, language: Languages, problem: Long) : Command() {
    private val code: String
    private val language: Languages
    private val problem: Problem
    private val output: StringBuilder

    init {
        this.event = event
        this.code = code
        this.language = language
        this.problem = Problem(problem)
        this.output = StringBuilder()
    }

    override fun setCommand() {
        val testCasesResults = Array(this.problem.size) { Pair(-1, -1) }
        this.event.channel.sendMessage(generateCodeRunningEmbed(testCasesResults, this.event).build())
            .queue { msg: Message? ->
                val folder = generateTempFolderAndAddFiles(this.code, this.language.fileExtension)
                for (i in 0 until this.problem.size) {
                    editInputFile(File("$folder/test.in"), problem.input[i])
                    this.output.clear()
                    msg!!.editMessage(generateCodeRunningEmbed(testCasesResults, this.event).build()).queue()
                    runTest(folder, testCasesResults, this.problem, i)
                    if (testCasesResults[i].first != 0) {
                        break
                    }
                }
                deleteFolderAndLog(folder)
                msg!!.editMessage(generateResultEmbed(testCasesResults, this.event).build()).queue()
            }
    }

    private fun runTest(folder: Path, testCases: Array<Pair<Int, Int>>, problem: Problem, index: Int) {
        val processBuilder = generateProcessBuilderAndLog(
            File("$folder/Main${this.language.fileExtension}"),
            File("$folder/test.in"),
            this.language
        )
        if (processBuilder == null) {
            testCases[index] = Pair(3, -1)
            return
        }
        val process = processBuilder.start()
        val outputReader = process.inputStream.bufferedReader()
        val updateOutput = Thread { readToStringBuilder(outputReader, this.output) }
        updateOutput.start()
        val startTime = System.currentTimeMillis()
        try {
            Awaitility.await().atMost(3, TimeUnit.SECONDS).until { !process.isAlive }
        } catch (e: ConditionTimeoutException) {
            testCases[index] = Pair(2, 3000)
        }
        val totalTime = (System.currentTimeMillis() - startTime).toInt()
        updateOutput.interrupt()
        process.destroyForcibly()
        process.waitFor()
        outputReader.close()
        testCases[index] = Pair(
            when {
                process.exitValue() != 0 -> 4
                this.output.toString().trim() == problem.output[index] -> 0
                else -> 1
            }, totalTime
        )
    }

    class Problem(problem: Long) {
        val problem: Long
        val size: Int
        val input: Array<String>
        val output: Array<String>

        init {
            this.problem = problem
            this.size = 4
            this.input = arrayOf("1", "2", "3", "4")
            this.output = arrayOf("1", "2", "6", "24")
        }
    }
}

private fun generateTempFolderAndAddFiles(code: String, extension: String): Path {
    val tempDir = createTempDirectory()
    val codeFile = File("$tempDir/Main${extension}")
    val inputFile = File("$tempDir/test.in")
    codeFile.deleteOnExit()
    inputFile.deleteOnExit()
    codeFile.writeText(code)
    inputFile.writeText("")
    return tempDir
}

private fun editInputFile(inputFile: File, input: String) {
    inputFile.writeText(input)
}

private fun generateProcessBuilderAndLog(codeFile: File, inputFile: File, language: Languages): ProcessBuilder? {
    val (runCommandBuilder, _) = language.compileAndReturnCommandAndSendErrorMessage.invoke(codeFile)
    if (runCommandBuilder == null) {
        Logger.getLogger(Judge::class.java.name)
            .log(Level.WARNING, "Compilation Error | File: ${codeFile.parentFile}")
        return null
    }
    runCommandBuilder.directory(codeFile.parentFile)
    runCommandBuilder.redirectInput(inputFile)
    return runCommandBuilder
}

private fun deleteFolderAndLog(folder: Path) {
    val file = folder.toFile()
    file.setWritable(true)
    if (!FileSystemUtils.deleteRecursively(file)) {
        Logger.getLogger(Judge::class.java.name)
            .log(Level.WARNING, "File Delete Failed: $file")
    }
}

private fun readToStringBuilder(out: BufferedReader, output: StringBuilder) {
    var stdout: Int
    while (out.read().also { stdout = it } != -1 && !Thread.currentThread().isInterrupted) {
        output.append(stdout.toChar())
    }
}

private fun generateCodeRunningEmbed(
    testCases: Array<Pair<Int, Int>>,
    event: MessageReceivedEvent
): EmbedBuilder {
    return EmbedBuilder()
        .setTitle("${event.author.name}'s Submission")
        .setDescription(IntStream.range(0, testCases.size).mapToObj { i: Int ->
            "**Test $i:**\n" + when (testCases[i].first) {
                -1 -> ":clock1: Pending"
                0 -> ":white_check_mark: Passed"
                1 -> ":x: Wrong Answer"
                2 -> ":warning: Time Limit Exceeded"
                else -> ":warning: Runtime or Compilation Error"
            } + " | ${testCases[i].second}ms"
        }.toArray().joinToString("\n"))
        .setFooter("Created by ${event.author.name}", event.author.avatarUrl)
}

private fun generateResultEmbed(testCases: Array<Pair<Int, Int>>, event: MessageReceivedEvent): EmbedBuilder {
    return EmbedBuilder()
        .setTitle(
            "${event.author.name}'s Submission - " +
                    if (Arrays.stream(testCases).anyMatch { a: Pair<Int, Int> -> a.first != 0 }) "Failed"
                    else "Completed"
        )
        .setDescription(IntStream.range(0, testCases.size).mapToObj { i: Int ->
            "**Test $i:**\n" + when (testCases[i].first) {
                -1 -> ":exclamation: Skipped"
                0 -> ":white_check_mark: Passed"
                1 -> ":x: Wrong Answer"
                2 -> ":warning: Time Limit Exceeded"
                else -> ":warning: Runtime or Compilation Error"
            } + " | ${testCases[i].second}ms"
        }.toArray().joinToString("\n"))
        .setFooter("Created by ${event.author.name}", event.author.avatarUrl)
}