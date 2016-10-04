# slather
Solving the Slather problem for COMS 4444.

## Collaborating on GitHub
Each of us can push our own code to our own branches (e.g. Avidan would push to a 'avidan' branch) and then create pull
requests to merge into master.

## Todo
* [x] Implement total cell count tracking
* [ ] Base strategy optimization to account for moves that would obstruct growth
* [ ] Base strategy optimization to account for pheromone avoidance
* [ ] Implement our own circling strategy based off of t
* [ ] Cleanup our code
* [ ] Implement quadrant-claiming start-game strategy
    - [ ] Have maxAngle algorithm handle additional quadrant argument
    - [ ] Implement quadrants
    - [ ] Test performance if children remain in their own quadrant vs. not

## Commands
    javac slather/sim/Simulator.java
    javac slather/g6/Player.java
    java slather.sim.Simulator --gui -g g6 g0 g0 g0 g0 g0 g0 g0 g0 g0

## Further Ideas
* Eights vs. circles vs. squares for late-game strategy

## Issues
* Our circling/squaring method isn't working
* Our code is messy