package stage2

import java.io.File

const val CONFIG_FILE_NAME = "./vcs/config.txt"
const val INDEX_FILE_NAME = "./vcs/index.txt"

fun main(args: Array<String>) {

    val svc = SystemVersionControl()

    if (args.isEmpty()) {
        svc.helpPage()
        return
    }

    when (args.firstOrNull()) {
        Commands.HELP.command -> svc.helpPage()
        Commands.CONFIG.command -> if (args.size == 1) svc.config() else svc.config(args.last())
        Commands.ADD.command -> if (args.size == 1) svc.add() else svc.add(args.last())
        Commands.LOG.command -> println("Show commit logs.")
        Commands.COMMIT.command -> println("Save changes.")
        Commands.CHECKOUT.command -> println("Restore a file.")
        else -> println("'${args.first()}' is not a SVCS command.")
    }
}

enum class Commands(val command: String) {
    HELP("--help"),
    CONFIG("config"),
    ADD("add"),
    LOG("log"),
    COMMIT("commit"),
    CHECKOUT("checkout"),
}

class SystemVersionControl {

    private lateinit var _username: String

    // only config has been entered
    // newUsername will override the
    fun config() {
        _username = getConfigFile().readText()
        if (_username.isEmpty()) {
            println("Please, tell me who you are.")
        } else {
            println("The username is $_username.")
        }
    }

    // 'config newUsername' has been entered,
    // 'newUsername' will overwrite username in configFile
    fun config(newUsername: String) {
        getConfigFile().writeText(newUsername)
        println("The username is $newUsername.")
    }

    fun add() {
        val indexedFiles = getIndexFile().readLines()
        if (indexedFiles.isEmpty()) println("Add a file to the index.")
        else {
            println("Tracked files:")
            indexedFiles.forEach { println(it) }
        }
    }

    // 'add newFile' has been entered
    // check if new file exists and append this file to index
    fun add(newFile: String) {
        if (File(newFile).exists()) {
            getIndexFile().appendText(newFile + '\n')
            println("The file '$newFile' is tracked.")
        } else {
            println("Can't find '$newFile'.")
        }
    }


    private fun getConfigFile(): File {
        return getFile(CONFIG_FILE_NAME)
    }

    private fun getIndexFile(): File {
        return getFile(INDEX_FILE_NAME)
    }

    // get the File, create it if it does not exist
    private fun getFile(fileName: String): File {
        val file = File(fileName)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        return file
    }

    fun helpPage() {
        println("These are SVCS commands:")
        println("config     Get and set a username.")
        println("add        Add a file to the index.")
        println("log        Show commit logs.")
        println("commit     Save changes.")
        println("checkout   Restore a file.")
    }
}