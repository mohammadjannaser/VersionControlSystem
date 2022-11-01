package stage4

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

const val VCS_FOLDER_NAME = "./vcs"
const val COMMITS_FOLDER_NAME = "$VCS_FOLDER_NAME/commits"
const val CONFIG_FILE_NAME = "$VCS_FOLDER_NAME/config.txt"
const val INDEX_FILE_NAME = "$VCS_FOLDER_NAME/index.txt"
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
        Commands.LOG.command -> svc.log()
        Commands.COMMIT.command -> if (args.size == 1) svc.commit() else svc.commit(args[1])
        Commands.CHECKOUT.command -> if (args.size == 1) svc.checkout() else svc.checkout(args[1])
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


    // checkout was called with commitId
    fun checkout(commitId: String) {
        val commitDir = File(COMMITS_FOLDER_NAME, commitId)
        if (!commitDir.exists()) {
            println("Commit does not exist.")
            return
        }

        commitDir.listFiles()?.forEach { file -> file.copyTo(File(".", file.name), overwrite = true) }
        println("Switched to commit $commitId.")
    }

    // checkout was called without argument
    fun checkout() {
        println("Commit id was not passed.")
    }

    // commit was called without message
    fun commit() {
        println("Message was not passed.")
    }

    // commit was called with message
    fun commit(message: String) {
        val filesInIndex = getIndexFile().readLines()

        val hash = checksum(filesInIndex)

        // check if files in index have changes,
        // we do this by comparing the current hash with the old/committed hash
        val logLines = getLogFile().readLines()
        if (logLines.isNotEmpty()) {
            val previousCommitHash = logLines.first().substring(7)
            if (hash == previousCommitHash) {
                println("Nothing to commit.")
                return
            }
        }

        // create hash folder if necessary
        val hashFolder = File(getCommitFolder(), hash)
        if (!hashFolder.exists()) {
            Files.createDirectory(Paths.get(hashFolder.toURI()))
        }

        // copy each file listed in index to commit/<hash> folder
        getIndexFile().readLines().forEach {
                indexedFile -> File(indexedFile).copyTo(File(hashFolder, indexedFile))
        }

        // weird: should we not clear index after all files from it were committed??
        //    getIndexFile().writeText("")

        writeMessageToLogFile(hash, message)

        println("Changes are committed.")
    }
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

    fun log() {
        val logLines = getLogFile().readLines()
        if (logLines.isEmpty()) {
            println("No commits yet.")
        } else {
            logLines.forEach { println(it) }
        }
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
        return getFile(CONFIG_FILE_NAME)
    }

    private fun getIndexFile(): File {
        return getFile(INDEX_FILE_NAME)
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