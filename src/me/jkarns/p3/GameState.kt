package me.jkarns.p3

import java.io.BufferedReader
import java.lang.Exception
import java.util.*
import java.util.concurrent.ForkJoinPool

class GameState(val width: Int, val height: Int, val ncolors: Int) {
    data class Node(val x: Int, val y: Int, val index: Int)

    init {
        assert(ncolors < 8)
    }

    companion object {
        const val FLOODED: Byte = -1
        var PARALELLISM: Int = Runtime.getRuntime().availableProcessors()
    }

    val maxX: Int = width - 1
    val maxY: Int = height - 1

    var floodedColor: Byte = 0

    val borderNodes: HashSet<Node> = HashSet(width * height)
    val graph: ByteArray = ByteArray(width * height)
    val tempSet: HashSet<Node> = HashSet(width * height)
    val colorCounts: IntArray = IntArray(ncolors)
    var atInitialState = true

    // Copy constructor
    constructor(other: GameState) : this(other.width, other.height, other.ncolors) {
        for (i in 0 until width * height)
            this.graph[i] = other.graph[i]
        for (i in other.borderNodes)
            this.borderNodes.add(i)
    }

    fun createRandomGraph() {
        val rng = Random(System.nanoTime())
        graph[0] = -1
        val seedNode = getNode(0, 0)
        borderNodes.add(seedNode)
        for (i in 1 until width * height) {
            graph[i] = (rng.nextInt().and(0xFFFF) % ncolors).toByte()
        }
    }

    fun getNode(x: Int, y: Int): Node {
        assert(x < width && y < height)
        return Node(x, y, x + y * width)
    }

    fun getNode(index: Int): Node {
        assert(index < width * height)
        return Node(index % width, index / width, index)
    }

    fun gc() {
        tempSet.clear()
        for (node in this.borderNodes)
            if (allNeighborsAreFlooded(node))
                tempSet.add(node)
        for (node in tempSet)
            borderNodes.remove(node)
    }

    fun getNeighbors(node: Node, buf: HashSet<Node>) {
        val (x, y, _) = node
        if (x > 0)      buf.add(getNode(x - 1, y))
        if (x < maxX)   buf.add(getNode(x + 1, y))
        if (y > 0)      buf.add(getNode(x, y - 1))
        if (y < maxY)   buf.add(getNode(x, y + 1))
    }

    fun allNeighborsAreFlooded(node: Node): Boolean {
        var result = true
        if (node.x > 0) result = result && this.graph[node.index - 1] == FLOODED
        if (result) {
            if (node.x < maxX) result = result && this.graph[node.index + 1] == FLOODED
            if (result) {
                if (node.y < maxY) result = result && this.graph[node.index + width] == FLOODED
                if (result) {
                    if (node.y > 0) result = result && this.graph[node.index - width] == FLOODED
                }
            }
        }
        return result
    }

    fun rootTally(): IntArray {
        val ret = IntArray(ncolors)
        ret[graph[1].toInt()] += 1
        ret[graph[width].toInt()] += 1
        return ret
    }

    fun tallyColors(nodes: HashSet<Node>) {
        for (i in 0 until ncolors)
            colorCounts[i] = 0
        for (node in nodes) {
            if (graph[node.index] < 0) continue
            colorCounts[graph[node.index].toInt()] += 1
        }
    }

    fun getNeighborsOfSameColor(node: Node): HashSet<Node> {
        val set = HashSet<Node>()
        getNeighborsOfSameColor(node, set)
        return set
    }

    fun getNeighborsOfSameColor(node: Node, neighbors: HashSet<Node>) {
        val color = graph[node.index]
        if (color == FLOODED) return
        if (node.y > 0) {
            val n = getNode(node.x, node.y - 1)
            if (graph[n.index] == color) {
                if (neighbors.add(n))
                    getNeighborsOfSameColor(n, neighbors)
            }
        }
        if (node.y < maxY) {
            val n = getNode(node.x, node.y + 1)
            if (graph[n.index] == color) {
                if (neighbors.add(n))
                    getNeighborsOfSameColor(n, neighbors)
            }
        }
        if (node.x > 0) {
            val n = getNode(node.x - 1, node.y)
            if (graph[n.index] == color) {
                if (neighbors.add(n))
                    getNeighborsOfSameColor(n, neighbors)
            }
        }
        if (node.x < maxX) {
            val n = getNode(node.x + 1, node.y)
            if (graph[n.index] == color) {
                if (neighbors.add(n))
                    getNeighborsOfSameColor(n, neighbors)
            }
        }
    }

    fun interpretMove(color: Byte) {
        assert(color > 0)
        atInitialState = false
        tempSet.clear()

        for (node in borderNodes)
            getNeighbors(node, tempSet)
        tallyColors(tempSet)
        for (node in tempSet)
            if (graph[node.index] == color) {
                val neighbors = getNeighborsOfSameColor(node)
                for (neighbor in neighbors)
                    graph[neighbor.index] = FLOODED
                borderNodes.addAll(neighbors)
                graph[node.index] = FLOODED
                borderNodes.add(node)
            }

        gc()
    }

    fun optimalMoveSequence(): Stack<Byte> {
        val fjp = ForkJoinPool(PARALELLISM)
        val r = if (atInitialState) fjp.invoke(Solver(this, null)) else fjp.invoke(Solver(this, -1))
        val st = Stack<Byte>()
        for (b in r) st.push(b)
        return st
    }

    fun displayLines(): Array<String> {
        var index = 0
        var result = Array(height) { _ -> "" }
        for (y in 0 until height) {
            var sb = StringBuilder(width * 6)
            for (_x in 0 until width) {
                val color = graph[index].toInt()
                if (color == -1) {
                    sb.append((0x1B).toChar())
                    sb.append('[')
                    sb.append(30)
                    sb.append('m')
                    sb.append('█')
                } else {
                    sb.append((0x1B).toChar())
                    sb.append('[')
                    sb.append(color + 30)
                    sb.append('m')
                    sb.append('#')
                }
                index += 1
            }
            result[y] = sb.toString()
        }
        return result
    }

    fun getKey(): String {
        val sb = StringBuilder()
        for (i in 0 until ncolors) {
            sb.append(0x1B.toChar())
            sb.append('[')
            sb.append(30 + i)
            sb.append('m')
            sb.append('█')
            sb.append(" = ")
            sb.append(i)
            sb.append(" ; ")
        }
        sb.append(0x1B.toChar())
        sb.append('[')
        sb.append(30)
        sb.append('m')
        sb.append("a = auto ; f = finish")
        return sb.toString()
    }

    fun done(): Boolean = borderNodes.size == 0

    fun moveCursor(x: Int, y: Int) {
        print(0x1B.toChar())
        print("[")
        print(y)
        print(';')
        print(x)
        print('H')
    }

    fun play() {
        var n = 0
        val key = getKey()
        var lastMove: Int = 0
        var soln = this.optimalMoveSequence()
        var finish = false
        var lastLine = "a"
        print(0x1B.toChar())
        print("[H")
        print(0x1B.toChar())
        println("[J")
        while (!done()) {
            moveCursor(0, 0)
            for (line in this.displayLines())
                println(line)
            println("$key ; Move Count: $n")
            if (finish) {
                this.interpretMove(soln.pop())
                Thread.sleep(50)
                continue
            }
            print("> ")
            var line: String = readLine() ?: return
            if (line == "") line = lastLine
            else lastLine = line
            var move = -1
            try {
                val i = Integer.parseInt(line)
                if (i < ncolors && i >= 0) {
                    move = i
                    lastMove = i
                }
            } catch (e: Exception) {
                val array = line.toCharArray()
                if (array[0] == 'a') {
                    if (lastMove != -1) {
                        soln = this.optimalMoveSequence()
                        lastMove = -1
                    }
                    move = soln.pop().toInt()
                } else if (array[0] == 'f') {
                    finish = true
                    soln = this.optimalMoveSequence()
                    continue
                }
            }
            if (move == -1) continue
            n += 1
            this.interpretMove(move.toByte())
        }
        moveCursor(0, 0)
        for (line in this.displayLines())
            println(line)
        println("Final score: $n (lower is better)")
    }
}