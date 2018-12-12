package me.jkarns.p3

import java.util.*
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class Solver(val gameState: GameState, val move: Byte?, val solnsFound: AtomicInteger) : RecursiveTask<ArrayList<Byte>>() {

    companion object {
        var THRESHOLD: Int = 1024 * 1024
    }

    constructor(gameState: GameState, move: Byte?) : this(gameState, move, AtomicInteger(0))

    override fun compute(): ArrayList<Byte>? {
        if (solnsFound.get() > THRESHOLD) return null
         if (move != null && move >= 0) {
             gameState.interpretMove(move)
            if (gameState.done()) {
                solnsFound.addAndGet(1)
                return arrayListOf<Byte>(move)
            }
         }
        val colorTally = if (move != null) gameState.colorCounts else gameState.rootTally()
        val moves = ArrayList<Int>(gameState.ncolors)
        for (i in 0 until colorTally.size)
            if (colorTally[i] != 0 && i.toByte() != move)
                moves.add(i)
        if (moves.size == 0)
            for (i in 0 until colorTally.size)
                if (colorTally[i] != 0)
                    moves.add(i)
        val tasks = ArrayList<Pair<ForkJoinTask<ArrayList<Byte>>, Byte>>(gameState.ncolors)

        if (moves.size == 0) {
            return null
        }

        for (i in 0 until moves.size)
            tasks.add(Pair(Solver(GameState(gameState), moves[i].toByte(), solnsFound).fork(), moves[i].toByte()))

        val results = ArrayList<Pair<ArrayList<Byte>?, Byte>>(gameState.ncolors)
//        results.add(Pair(Solver(GameState(gameState), moves[0].toByte(), solnsFound).compute(), moves[0].toByte()))

        for (task in tasks)
            results.add(Pair(task.first.join(), task.second))

        results.sortBy { a ->
            if (a.first == null)
                Int.MAX_VALUE
            else
                a.first!!.size
        }
        if (move != null) results[0].first?.add(move)

        return results[0].first
    }

}
