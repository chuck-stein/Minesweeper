import java.util.ArrayList;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/*-
 * EXTRA FEATURES:
 * 
 * time counter
 * mines remaining counter
 * opens all cells upon loss
 * open unflagged neighbors when clicking on a cell whose surrounding flags = surrounding mines
 * restart the game by pressing "R" during your current game
 * restart the game automatically upon loss if you uncomment line 92
 * 
 */

// NOTE: fields of fields are only used when the exact, specific type of the field
// whose field you're accessing is known

// Represents a game of Minesweeper
class Game extends World {
  Random rand;
  int numMines;
  int width;
  int height;
  Grid grid;
  int tickNum;

  Game(Random rand, int numMines, int width, int height) {
    this.rand = rand;
    this.numMines = numMines;
    this.width = width;
    this.height = height;
    this.grid = new Grid(this.rand, this.width, this.height, this.numMines);
    this.tickNum = 0;
  }

  Game(int numMines, int width, int height) {
    this(new Random(), numMines, width, height);
  }

  // draws the game of Minesweeper to be displayed on every tick
  public WorldScene makeScene() {
    WorldScene scene = this.grid.draw();
    scene.placeImageXY(
        new TextImage("Mines left: " + (this.numMines - this.grid.numFlags()), 24, Color.BLACK),
        this.width * 30 / 4, this.height * 30 + 30);
    scene.placeImageXY(new TextImage("Time: " + this.tickNum, 24, Color.BLACK),
        this.width * 30 / 4 * 3, this.height * 30 + 30);
    return scene;
  }

  // returns the game over display, which reveals all mines
  // and notifies the player of their loss
  // EFFECT: opens all cells
  public WorldScene lastScene(String msg) {
    this.grid.openAll();
    WorldScene scene = this.grid.draw();
    Color color = Color.RED;
    if (msg.equals("YOU WIN!")) {
      color = Color.BLUE;
    }
    scene.placeImageXY(new TextImage(msg, 48, color), scene.width / 2, scene.height / 2);
    scene.placeImageXY(
        new TextImage("Mines left: " + (this.numMines - this.grid.numFlags()), 24, Color.BLACK),
        scene.width / 4, scene.height + 30);
    scene.placeImageXY(new TextImage("Time: " + this.tickNum, 24, Color.BLACK), scene.width / 4 * 3,
        scene.height + 30);
    return scene;
  }

  // on every tick, increases the counter for ticks so far in this game
  public void onTick() {
    this.tickNum += 1;
  }

  // EFFECT: handles mouse clicks to open or flag cells,
  // and informs the player of a win or loss if necessary
  public void onMouseClicked(Posn p, String button) {
    if (p.x > 0 && p.x < this.width * 30 && p.y > 0 && p.y < this.height * 30) {
      if (button.equals("LeftButton")) {
        this.grid.openCell(p);
      }
      else if (button.equals("RightButton")) {
        this.grid.toggleFlag(p);
      }
      if (this.grid.gameOver()) {
        this.endOfWorld("GAME OVER");
        // this.onKeyEvent("r");
      }
      else if (this.grid.win()) {
        this.endOfWorld("YOU WIN!");
      }
    }
  }

  // EFFECT: Handles key events, specifically restarting the game
  // when the user presses "R"
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.grid = new Grid(new Random(), this.width, this.height, this.numMines);
      this.tickNum = 0;
    }
  }

}

// Represents a grid of cells across which mines are randomly dispersed
class Grid {
  ArrayList<ArrayList<Cell>> cells;

  Grid(Random rand, int width, int height, int numMines) {
    this.cells = new ArrayList<ArrayList<Cell>>();

    // builds a grid of dummy cells
    for (int y = 0; y < height; y++) {
      this.cells.add(new ArrayList<Cell>());
      for (int x = 0; x < width; x++) {
        this.cells.get(y).add(new Cell(false, new ArrayList<Cell>()));
      }
    }

    // sets the correct neighbors for each cell
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        this.cells.get(y).get(x).setNeighbors(this.findNeighbors(x, y));
      }
    }

    this.placeMines(rand, numMines);
  }

  // constructor used only for testing placeMines (creates a grid with no mines)
  Grid(int width, int height) {
    this.cells = new ArrayList<ArrayList<Cell>>();

    // builds a grid of dummy cells
    for (int y = 0; y < height; y++) {
      this.cells.add(new ArrayList<Cell>());
      for (int x = 0; x < width; x++) {
        this.cells.get(y).add(new Cell(false, new ArrayList<Cell>()));
      }
    }
  }

  // returns a list of every Cell adjacent (in any of the 8 directions) to the one
  // at the given coordinates
  ArrayList<Cell> findNeighbors(int x, int y) {
    ArrayList<Cell> neighbors = new ArrayList<Cell>();
    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        if (!(i == 0 && j == 0) && this.inBounds(x + j, y + i)) {
          neighbors.add(this.cells.get(y + i).get(x + j));
        }
      }
    }
    return neighbors;
  }

  // is there a Cell at the given coordinates on this Grid?
  boolean inBounds(int x, int y) {
    return y >= 0 && y < this.cells.size() && x >= 0 && x < this.cells.get(y).size();
  }

  // EFFECT: randomly places the given number of mines into Cells on this Grid,
  // never repeating a Cell
  void placeMines(Random rand, int numMines) {
    int x;
    int y;
    while (numMines > 0) {
      y = rand.nextInt(this.cells.size());
      x = rand.nextInt(this.cells.get(y).size());
      if (!this.cells.get(y).get(x).hasMine) {
        this.cells.get(y).get(x).placeMine();
        numMines--;
      }
    }
  }

  // counts the flags in this grid
  int numFlags() {
    int count = 0;
    for (ArrayList<Cell> a : this.cells) {
      for (Cell c : a) {
        if (c.flagged) {
          count += 1;
        }
      }
    }
    return count;
  }

  // EFFECT: opens the Cell at the given Posn
  void openCell(Posn p) {
    this.cells.get(p.y / 30).get(p.x / 30).open(true);
  }

  // EFFECT: toggles the flag on the Cell at the given Posn
  void toggleFlag(Posn p) {
    this.cells.get(p.y / 30).get(p.x / 30).toggleFlag();
  }

  // do any of the open Cells in this Grid contain a mine, thus ending the game?
  boolean gameOver() {
    for (ArrayList<Cell> a : this.cells) {
      for (Cell c : a) {
        if (c.gameOver()) {
          return true;
        }
      }
    }
    return false;
  }

  // are all non-mine cells open?
  boolean win() {
    for (ArrayList<Cell> a : this.cells) {
      for (Cell c : a) {
        if (!(c.open || c.hasMine)) {
          return false;
        }
      }
    }
    return true;
  }

  // EFFECT: opens every Cell in this Grid
  void openAll() {
    for (ArrayList<Cell> a : this.cells) {
      for (Cell c : a) {
        c.open(false);
      }
    }
  }

  // draws every Cell in this Grid at its coordinates
  WorldScene draw() {
    WorldScene scene = new WorldScene(this.cells.size() * 30, this.cells.get(0).size() * 30);
    for (int y = 0; y < this.cells.size(); y++) {
      for (int x = 0; x < this.cells.get(y).size(); x++) {
        scene.placeImageXY(this.cells.get(y).get(x).draw(), x * 30 + 15, y * 30 + 15);
      }
    }
    return scene;
  }

}

// Represents a tile in Minesweeper, which may or may not be open, flagged, or
// containing a mine
class Cell {
  boolean hasMine;
  boolean open;
  boolean flagged;
  ArrayList<Cell> neighbors;

  Cell(boolean hasMine, ArrayList<Cell> neighbors) {
    this.hasMine = hasMine;
    this.open = false;
    this.flagged = false;
    this.neighbors = neighbors;
  }

  // finds the number of mines adjacent to this Cell
  int surroundingMines() {
    int num = 0;
    for (Cell c : this.neighbors) {
      if (c.hasMine) {
        num++;
      }
    }
    return num;
  }

  // finds the number of flagged Cells adjacent to this Cell
  int surroundingFlags() {
    int num = 0;
    for (Cell c : this.neighbors) {
      if (c.flagged) {
        num++;
      }
    }
    return num;
  }

  // EFFECT: changes this cell's list of neighbors to the given list
  void setNeighbors(ArrayList<Cell> neighbors) {
    this.neighbors = neighbors;
  }

  // EFFECT: this cell now contains a mine
  void placeMine() {
    this.hasMine = true;
  }

  // EFFECT: opens this cell, and any neighbors if its surrounding mine count is
  // equal to 0 or its surrounding flags
  void open(boolean cellWasClicked) {
    this.open = true;
    if (this.surroundingMines() == 0) {
      for (Cell c : this.neighbors) {
        if (!c.open && !flagged) {
          c.open(false);
        }
      }
    }
    if (cellWasClicked && this.surroundingMines() == this.surroundingFlags()) {
      for (Cell c : this.neighbors) {
        if (!c.flagged) {
          c.open(false);
        }
      }
    }
  }

  // should this cell cause the game to end
  // because it is open and containing a mine?
  boolean gameOver() {
    return this.open && this.hasMine;
  }

  // EFFECT: flags this cell if it is unflagged, removes the flag if it is flagged
  void toggleFlag() {
    this.flagged = !this.flagged;
  }

  // draws this cell, open or closed, with a flag or mine or # of adjacent mines
  // if applicable
  WorldImage draw() {
    WorldImage img = new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.BLUE);
    if (this.open) {
      if (this.hasMine) {
        img = new OverlayImage(new CircleImage(10, OutlineMode.SOLID, Color.BLACK), img);
      }
      else if (this.surroundingMines() > 0) {
        img = new OverlayImage(new TextImage("" + this.surroundingMines(), 24, Color.GREEN), img);
      }
    }
    else {
      img = new OverlayImage(img, new RectangleImage(30, 30, OutlineMode.SOLID, Color.LIGHT_GRAY));
      if (this.flagged) {
        img = new OverlayImage(new TriangleImage(new Posn(0, -10), new Posn(-10, 10),
            new Posn(10, 10), OutlineMode.SOLID, Color.RED), img);
      }
    }
    return img;
  }

}

class ExamplesMinesweeper {
  Random testRand;
  Game world1;
  Game world2;
  Game randomWorld;
  WorldImage cellBorder;
  WorldImage img1;
  WorldImage img2;
  WorldImage img3;
  WorldImage img4;
  WorldScene scene1;
  WorldScene scene2;
  WorldScene scene3;
  Grid grid1;
  Grid grid2;
  Cell cell1;
  Cell cell2;
  Cell cell3;
  Cell cell4;
  Cell cell5;
  ArrayList<Cell> neighbors;
  ArrayList<Cell> neighbors2;

  void init() {
    this.testRand = new Random(12345);
    this.world1 = new Game(this.testRand, 60, 20, 20);
    this.world2 = new Game(this.testRand, 2, 2, 2);
    this.world2.grid.cells.get(0).get(0).open = true;
    this.world2.grid.cells.get(0).get(1).open = true;
    this.world2.grid.cells.get(1).get(0).open = true;
    this.world2.grid.cells.get(1).get(1).flagged = true;
    // change the following values to modify default mines, width, and height
    this.randomWorld = new Game(40, 15, 15);

    this.cellBorder = new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.BLUE);
    this.img1 = new OverlayImage(new TextImage("2", 24, Color.GREEN), cellBorder);
    this.img2 = new OverlayImage(new CircleImage(10, OutlineMode.SOLID, Color.BLACK), cellBorder);
    this.img3 = new OverlayImage(
        new TriangleImage(new Posn(0, -10), new Posn(-10, 10), new Posn(10, 10), OutlineMode.SOLID,
            Color.RED),
        new OverlayImage(cellBorder,
            new RectangleImage(30, 30, OutlineMode.SOLID, Color.LIGHT_GRAY)));

    this.scene1 = new WorldScene(60, 60);
    this.scene2 = new WorldScene(60, 60);
    this.scene3 = new WorldScene(60, 60);
    this.scene1.placeImageXY(this.img1, 15, 15);
    this.scene2.placeImageXY(this.img1, 15, 15);
    this.scene3.placeImageXY(this.img1, 15, 15);
    this.scene1.placeImageXY(this.img1, 45, 15);
    this.scene2.placeImageXY(this.img1, 45, 15);
    this.scene3.placeImageXY(this.img1, 45, 15);
    this.scene1.placeImageXY(this.img2, 15, 45);
    this.scene2.placeImageXY(this.img2, 15, 45);
    this.scene3.placeImageXY(this.img2, 15, 45);
    this.scene1.placeImageXY(this.img3, 45, 45);
    this.scene2.placeImageXY(this.img2, 45, 45);
    this.scene3.placeImageXY(this.img2, 45, 45);
    this.scene2.placeImageXY(new TextImage("GAME OVER", 48, Color.RED), this.scene2.width / 2,
        this.scene2.height / 2);
    this.scene3.placeImageXY(new TextImage("YOU WIN!", 48, Color.BLUE), this.scene3.width / 2,
        this.scene3.height / 2);

    this.grid1 = new Grid(this.testRand, 20, 20, 60);
    this.grid2 = new Grid(5, 5);

    this.cell1 = new Cell(false, new ArrayList<Cell>());
    this.cell2 = new Cell(true, new ArrayList<Cell>());
    this.cell3 = new Cell(false, this.neighbors);
    this.cell4 = new Cell(true, this.neighbors2);
    this.cell5 = new Cell(true, this.neighbors);

    this.neighbors = new ArrayList<Cell>();
    this.neighbors.add(this.grid1.cells.get(4).get(3));
    this.neighbors.add(this.grid1.cells.get(4).get(4));
    this.neighbors.add(this.grid1.cells.get(4).get(5));
    this.neighbors.add(this.grid1.cells.get(5).get(3));
    this.neighbors.add(this.grid1.cells.get(5).get(5));
    this.neighbors.add(this.grid1.cells.get(6).get(3));
    this.neighbors.add(this.grid1.cells.get(6).get(4));
    this.neighbors.add(this.grid1.cells.get(6).get(5));

    this.neighbors2 = new ArrayList<Cell>();
    this.neighbors2.add(this.grid1.cells.get(0).get(18));
    this.neighbors2.add(this.grid1.cells.get(1).get(18));
    this.neighbors2.add(this.grid1.cells.get(1).get(19));
  }

  // MAIN METHOD FOR PLAYING THE GAME
  // CHANGE "world" VARIABLE TO PLAY A DIFFERENT WORLD
  void testBigBang(Tester t) {
    this.init();
    Game world = this.randomWorld;
    world.bigBang(world.grid.cells.get(0).size() * 30, world.grid.cells.size() * 30 + 60, 1.0);
  }

  void testMakeScene(Tester t) {
    this.init();
    t.checkExpect(this.world2.makeScene(), this.scene1);
  }

  void testLastScene(Tester t) {
    this.init();
    t.checkExpect(world2.lastScene("GAME OVER"), this.scene2);
    t.checkExpect(world2.lastScene("YOU WIN!"), this.scene3);

    // BEFORE CHANGE
    t.checkExpect(this.world1.grid.cells.get(0).get(0).open, false);

    // CHANGE
    this.world1.lastScene("GAME OVER");

    // AFTER CHANGE
    t.checkExpect(this.world1.grid.cells.get(0).get(0).open, true);
  }

  void testOnTick(Tester t) {
    this.init();

    t.checkExpect(this.randomWorld.tickNum, 0); // BEFORE CHANGE
    this.randomWorld.onTick(); // CHANGE 1
    t.checkExpect(this.randomWorld.tickNum, 1); // AFTER CHANGE 1
    this.randomWorld.onTick(); // CHANGE 2
    t.checkExpect(this.randomWorld.tickNum, 2); // AFTER CHANGE 2
  }

  void testOnMouseClicked(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, false);
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, true);

    // CHANGE 1
    this.world2.onMouseClicked(new Posn(32, 40), "RightButton");
    this.world2.onMouseClicked(new Posn(38, 11), "RightButton");

    // AFTER CHANGE 1
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, false);

    // CHANGE 2
    this.world2.onMouseClicked(new Posn(32, 40), "LeftButton");
    this.world2.onMouseClicked(new Posn(5, 3), "LeftButton");

    // AFTER CHANGE 2
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, true);
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);
  }

  void testOnKeyEvent(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(0).open, true);

    // CHANGE 1
    this.world2.onKeyEvent("q");

    // AFTER CHANGE 1
    t.checkExpect(this.world2.grid.cells.get(1).get(0).open, true);

    // CHANGE 2
    this.world2.onKeyEvent("r");

    // AFTER CHANGE 2
    t.checkExpect(this.world2.grid.cells.get(1).get(0).open, false);
  }

  void testFindNeighbors(Tester t) {
    this.init();
    t.checkExpect(this.grid1.findNeighbors(4, 5), this.neighbors);
    t.checkExpect(this.grid1.findNeighbors(19, 0), this.neighbors2);
  }

  void testInBounds(Tester t) {
    this.init();
    t.checkExpect(this.grid1.inBounds(11, 7), true);
    t.checkExpect(this.grid1.inBounds(0, 19), true);
    t.checkExpect(this.grid1.inBounds(-3, 12), false);
    t.checkExpect(this.grid1.inBounds(6, -88), false);
    t.checkExpect(this.grid1.inBounds(20, 20), false);
  }

  void testPlaceMines(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.grid2.cells.get(3).get(2).hasMine, false);
    t.checkExpect(this.grid2.cells.get(0).get(0).hasMine, false);

    // CHANGE
    this.grid2.placeMines(this.testRand, 15);

    // AFTER CHANGE
    t.checkExpect(this.grid2.cells.get(3).get(2).hasMine, true);
    t.checkExpect(this.grid2.cells.get(0).get(0).hasMine, false);
  }

  void testNumFlags(Tester t) {
    this.init();
    t.checkExpect(this.world1.grid.numFlags(), 0);
    t.checkExpect(this.world2.grid.numFlags(), 1);
  }

  void testOpenCell(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, false);
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);

    // CHANGE
    this.world2.grid.openCell(new Posn(1, 10));
    this.world2.grid.openCell(new Posn(52, 45));

    // AFTER CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, true);
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);
  }

  void testToggleFlagGrid(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, true);
    t.checkExpect(this.world2.grid.cells.get(0).get(1).flagged, false);

    // CHANGE
    this.world2.grid.toggleFlag(new Posn(50, 50));
    this.world2.grid.toggleFlag(new Posn(43, 16));

    // AFTER CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, false);
    t.checkExpect(this.world2.grid.cells.get(0).get(1).flagged, true);
  }

  void testGameOverGrid(Tester t) {
    this.init();
    t.checkExpect(this.world1.grid.gameOver(), false);
    t.checkExpect(this.world2.grid.gameOver(), true);
  }

  void testWin(Tester t) {
    this.init();
    t.checkExpect(this.world1.grid.win(), false);
    t.checkExpect(this.world2.grid.win(), true);
  }

  void testOpenAll(Tester t) {
    this.init();

    // BEFORE CHANGE
    for (ArrayList<Cell> a : this.world1.grid.cells) {
      for (Cell c : a) {
        t.checkExpect(c.open, false);
      }
    }

    // CHANGE
    this.world1.grid.openAll();

    // AFTER CHANGE
    for (ArrayList<Cell> a : this.world1.grid.cells) {
      for (Cell c : a) {
        t.checkExpect(c.open, true);
      }
    }
  }

  void testDrawGrid(Tester t) {
    this.init();
    t.checkExpect(this.world2.grid.draw(), this.scene1);
  }

  void testSurroundingMines(Tester t) {
    this.init();
    t.checkExpect(this.world1.grid.cells.get(1).get(2).surroundingMines(), 0);
    t.checkExpect(this.world1.grid.cells.get(0).get(10).surroundingMines(), 2);
    t.checkExpect(this.world1.grid.cells.get(17).get(1).surroundingMines(), 5);
    t.checkExpect(this.world1.grid.cells.get(16).get(1).surroundingMines(), 4);
  }

  void testSurroundingFlags(Tester t) {
    this.init();
    t.checkExpect(this.world1.grid.cells.get(12).get(6).surroundingFlags(), 0);
    t.checkExpect(this.world2.grid.cells.get(0).get(0).surroundingFlags(), 1);
  }

  void testSetNeighbors(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.cell1.neighbors, new ArrayList<Cell>());
    t.checkExpect(this.cell2.neighbors, new ArrayList<Cell>());
    t.checkExpect(this.cell3.neighbors, this.neighbors);
    t.checkExpect(this.cell4.neighbors, this.neighbors2);

    // CHANGE
    this.cell1.setNeighbors(this.neighbors);
    this.cell2.setNeighbors(this.neighbors2);
    this.cell3.setNeighbors(new ArrayList<Cell>());
    this.cell4.setNeighbors(this.neighbors2);

    // AFTER CHANGE
    t.checkExpect(this.cell1.neighbors, this.neighbors);
    t.checkExpect(this.cell2.neighbors, this.neighbors2);
    t.checkExpect(this.cell3.neighbors, new ArrayList<Cell>());
    t.checkExpect(this.cell4.neighbors, this.neighbors2);

  }

  void testPlaceMine(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.cell1.hasMine, false);
    t.checkExpect(this.cell2.hasMine, true);

    // CHANGE
    this.cell1.placeMine();
    this.cell2.placeMine();

    // AFTER CHANGE
    t.checkExpect(this.cell1.hasMine, true);
    t.checkExpect(this.cell2.hasMine, true);
  }

  void testOpen(Tester t) {
    this.init();

    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, false);

    // CHANGE
    this.world2.grid.cells.get(0).get(0).open(false);
    this.world2.grid.cells.get(1).get(1).open(true);

    // AFTER CHANGE
    t.checkExpect(this.world2.grid.cells.get(0).get(0).open, true);
    t.checkExpect(this.world2.grid.cells.get(1).get(1).open, true);
  }

  void testGameOverCell(Tester t) {
    this.init();
    t.checkExpect(this.world2.grid.cells.get(1).get(1).gameOver(), false);
    t.checkExpect(this.world2.grid.cells.get(1).get(0).gameOver(), true);
    t.checkExpect(this.world2.grid.cells.get(0).get(1).gameOver(), false);

  }

  void testToggleFlagCell(Tester t) {
    this.init();
    // BEFORE CHANGE
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, true);

    // CHANGE 1
    this.world2.grid.cells.get(1).get(1).toggleFlag();

    // AFTER CHANGE 1
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, false);

    // CHANGE 2
    this.world2.grid.cells.get(1).get(1).toggleFlag();

    // AFTER CHANGE 2
    t.checkExpect(this.world2.grid.cells.get(1).get(1).flagged, true);
  }

  void testDrawCell(Tester t) {
    this.init();
    t.checkExpect(this.world2.grid.cells.get(0).get(0).draw(), this.img1);
    t.checkExpect(this.world2.grid.cells.get(0).get(1).draw(), this.img1);
    t.checkExpect(this.world2.grid.cells.get(1).get(0).draw(), this.img2);
    t.checkExpect(this.world2.grid.cells.get(1).get(1).draw(), this.img3);
  }

}