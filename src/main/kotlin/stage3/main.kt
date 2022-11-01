package stage3

import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

const val VCS_FOLDER_NAME = "./vcs"
const val COMMITS_FOLDER_NAME = "$VCS_FOLDER_NAME/commits"
const val LOG_FILE_NAME = "$VCS_FOLDER_NAME/log.txt"
const val HASH_ALGORITHM = "SHA3-256"


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

    private fun writeMessageToLogFile(hash: String, message: String) {
        // add new commit message at the top of log file (unfortunately there is no 'appendAtTop' or so)
        val oldLog = getLogFile().readText()
        getLogFile().writeText(buildLogMessage(hash, message) + "\n" + oldLog)
    }

    private fun buildLogMessage(hash: String, commitMessage: String): String {
        val username = getConfigFile().readText()
        return """
        commit $hash
        Author: $username
        $commitMessage
    """.trimIndent()
    }


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
        return getFile(stage2.CONFIG_FILE_NAME)
    }

    private fun getIndexFile(): File {
        return getFile(stage2.INDEX_FILE_NAME)
    }

    private fun getLogFile(): File {
        return getFile(LOG_FILE_NAME)
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

    private fun getCommitFolder(): File {
        val commitFolder = File(COMMITS_FOLDER_NAME)
        if (!commitFolder.exists()) {
            commitFolder.mkdirs()
        }
        return commitFolder
    }

    fun helpPage() {
        println("These are SVCS commands:")
        println("config     Get and set a username.")
        println("add        Add a file to the index.")
        println("log        Show commit logs.")
        println("commit     Save changes.")
        println("checkout   Restore a file.")
    }

    private fun digestFiles(files: List<String>): ByteArray {
        var md = MessageDigest.getInstance(HASH_ALGORITHM)
        files.forEach {
                file -> val dis = DigestInputStream(FileInputStream(file), md)
            while (dis.read() != -1);
            md = dis.messageDigest
        }

        return md.digest()
    }
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { eachByte -> "%02x".format(eachByte) }
    }

    private fun checksum(files: List<String>): String {
        return bytesToHex(digestFiles(files))
    }

}