package stage1

fun main(args: Array<String>) {

    when {
        args.isEmpty() -> helpPage()
        args.first().equals("--help",ignoreCase = true) -> helpPage()
        isCommandExist(args.first()) -> commandDescription(args.first())
        else -> println("'${args.first()}' is not a SVCS command.")
    }
}

private val commands = mapOf(
    Pair("config","Get and set a username."),
    Pair("add","Add a file to the index."),
    Pair("log","Show commit logs."),
    Pair("commit","Save changes."),
    Pair("checkout","Restore a file."),
)

fun helpPage(){
    println("These are SVCS commands:")
    println("config     Get and set a username.")
    println("add        Add a file to the index.")
    println("log        Show commit logs.")
    println("commit     Save changes.")
    println("checkout   Restore a file.")
}

private fun isCommandExist(command: String) = commands.any { it.key == command }

private fun commandDescription(command: String) = println(commands[command])
