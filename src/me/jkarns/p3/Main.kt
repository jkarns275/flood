package me.jkarns.p3

import java.util.*

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size == 0) {
            println("Provide a grid size formated as CxNxM (e.g. 5x10x10).")
            return
        }
        if (args.size >= 2) {
            Solver.THRESHOLD = Integer.parseInt(args[1])
        }
        if (args.size >= 3) {
            GameState.PARALELLISM = Integer.parseInt(args[2])
        }
        val split = args[0].split('x')
        val c = Integer.parseInt(split[0])
        val n = Integer.parseInt(split[1])
        val m = Integer.parseInt(split[2])
        val gameState = GameState(n, m, c)
        gameState.createRandomGraph()
        gameState.play()
    }
}