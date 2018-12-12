# flood
An implementation of the game 'Flood' in Kotlin that can be played in the terminal.
Reference implementation: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/flood.html

This implementation of flood is to be ran in the terminal. It has a solver that runs in paralell.
The grid size, number of colors, solver depth, and solver paralellism (i.e. number of threads).

To run:
```
java -jar <jarfile> <colors>x<columns>x<rows> <solver depth> <paralellism>
```

So for example, to run with a grid 32 cells wide and 16 cells tall with 4 colors, solver depth of 1000, and paralellism of 4
you would type:
```
java -jar <jarfile> 4x32x16 1000 4
```
