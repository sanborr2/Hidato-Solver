/**
 * Author :  Robert Sanborn
 * File :    Hidato
 * Purpose : CSCI 145, Assignment 1
 * Date :    1/26/17
 */


import java.io.*;
import java.util.*;

public class Hidato {

    // Usage Message
    static final String usage = "Usage: Hidato input_file_name delay_Time";

    // Input file object.
    static File inputFile;
    static int delayTime;

    static int heightPuzzle;
    static int widthPuzzle;
    static int highestNum;

    static int[][] locationGrid;
    static int[][] valueTable;

    static int iterations;

    static DrawGrid g;


    public static void main(String[] args) throws FileNotFoundException{
        if(!processArgs(args)){
            System.out.println(usage);
        }

        readGrid(new Scanner(new File(args[0])));

        g = new DrawGrid(heightPuzzle,widthPuzzle);
        g.setDelay(delayTime);
        for (int row = 0; row < heightPuzzle; row++){
            for (int col = 0; col < widthPuzzle; col++) {
                if (locationGrid[row][col] > 0) {
                    g.fixedCell(row,col,locationGrid[row][col]);

                } else if (locationGrid[row][col] == 0){
                    g.valueCell(row, col);

                } else{
                    g.emptyCell(row,col);
                }
            }
        }
        g.draw();


        if(explore()) {
            System.out.println("The Hidato puzzle has been solved!");
            System.out.println("It took " + iterations + " steps to solve!");

        } else {
            System.out.println("The Hidato puzzle has not been solved!");
            System.out.println(iterations + " steps have been taken");
        }
    }


    public static boolean processArgs(String[] args){
        // Check for correct number of arguments
        if (args.length != 2) {
            System.out.println("Wrong number of command line arguments.");
            System.out.println(usage);
            return false;
        }
        // Open the input file and get its length
        inputFile = new File(args[0]);
        if (!inputFile.canRead()) {
            System.out.println("The file " + args[0]
                    + " cannot be opened for input.");
            return false;
        }

        try {
             delayTime = Integer.parseInt(args[1]);

        } catch (NumberFormatException ex) {
            System.out.println("delay_Time must be an integer.");
            System.out.println(usage);
            return false;
        }
        return true;
    }

    public static void readGrid(Scanner input){
        String line = input.nextLine();
        Scanner scan = new Scanner(line);

        heightPuzzle = scan.nextInt();
        widthPuzzle = scan.nextInt();
        highestNum = -1;

        locationGrid = new int[heightPuzzle][widthPuzzle];
        for(int r = 0; r < heightPuzzle; r++){
            Scanner row = new Scanner(input.nextLine());
            for (int c = 0; c < widthPuzzle; c++){
                if (row.hasNext()){
                    if (row.hasNextInt()){
                        locationGrid[r][c] = row.nextInt();
                        if (locationGrid[r][c] > highestNum){
                            highestNum = locationGrid[r][c];
                        }
                    } else if((row.next().equals("x"))) {
                        locationGrid[r][c] = -1;
                    }
                }
            }
        }

        valueTable = new int[highestNum][2];
        for(int r = 0; r < heightPuzzle; r++) {
            for (int c = 0; c < widthPuzzle; c++) {
                if (locationGrid[r][c] > 0) {
                    valueTable[locationGrid[r][c] - 1][0] = r;
                    valueTable[locationGrid[r][c] - 1][1] = c;
                }
            }
        }

        for (int[] value: valueTable){
            if (value[0] == 0 && value[1] == 0){
                value[0] = -1;
                value[1] = -1;
            }
        }

        if (locationGrid[0][0] > 0){
            valueTable[locationGrid[0][0] -1][0] = 0;
            valueTable[locationGrid[0][0] -1][1] = 0;
        }


    }

    public static boolean explore(){
        iterations++;

        // Find the next missing value, that is, the next number that
        // has not already been placed in the grid
        int next = findMissingValue();

        // If no missing value was found, we’ve succeeded. Return
        // true indicating success
        if (next == -1) {
            return true;
        }

        // Find the next known value. That is, the next value already
        // placed in the grid
        int end = findNextValue(next);

        // start is the value prior to next. This value is guaranteed
        // to have already been placed in the grid
        int start = next - 1;

//        System.out.println(next + " " + end + " " + start);

        // Loop through the eight possible adjacent locations
        // to the location of start

        for (int nextLoc[]: adjacentCells(start)) {

            // Test to see if nextLoc is a viable location for
            // the value next. There are three conditions that
            // must be satisfied:
            //   1. nextLoc must actually be inside the
            //   2. the grid cell at nextLoc must be available
            //      to have next placed there
            //   3. the grid distance from nextLoc to the grid
            //      location of end must not be too great

            if ( (nextLoc[0] >= 0 && nextLoc[0] < heightPuzzle) &&
                    (nextLoc[1] >= 0 && nextLoc[1] < widthPuzzle) &&
                    (locationGrid[nextLoc[0]][nextLoc[1]] == 0) &&
                    (distance(nextLoc, valueTable[end-1]) <= end - next) ) {


                // Assuming that everything looks good place
                // next at nextLoc and explore again
                place(nextLoc, next);


                if (explore()) {
                    // The search succeeded, return true
                    return true;
                }

                // The search did not succeed, remove next from
                // nextLoc and try the next adjacent cell
                remove(nextLoc, next);
            }

        }
        // At this point we have failed, return false indicating failure
        return false;
    }

    public static int findMissingValue(){

        for(int index = 0; index < valueTable.length; index++){
            if(valueTable[index][0] == -1){
                return index + 1;
            }
        }
        return -1;
    }

    public static int findNextValue(int next){
        for(int index = next; index < valueTable.length; index++){
            if(valueTable[index][0] != -1){
                return index + 1;
            }
        }
        return -1;
    }

    public static int[][] adjacentCells(int start){
        int[] startCoordinate = valueTable[start -1];

        int[][] possibleCoordinates = new int[8][2];

        int i = 0;
        for(int[] coordinate: possibleCoordinates){
            coordinate[0] = (startCoordinate[0]
                    + (int)Math.round(Math.cos(Math.PI/4 * i)));

            coordinate[1] = (startCoordinate[1]
                    + (int)Math.round(Math.sin(Math.PI/4 * i)));

            i++;
        }

//        System.out.println(Arrays.deepToString(possibleCoordinates));
        return possibleCoordinates;
    }

    public static int distance(int[] initialLoc, int[] endLoc){
        return Math.max(Math.abs(initialLoc[0] - endLoc[0]),
                        Math.abs(initialLoc[1] - endLoc[1]));
    }



    public static void place(int[] nextLoc, int next){
        locationGrid[nextLoc[0]][nextLoc[1]] = next;

        valueTable[next-1][0] = nextLoc[0];
        valueTable[next-1][1] = nextLoc[1];

        g.fillCell(nextLoc[0], nextLoc[1], next);
//        System.out.println(nextLoc[0] + " " + nextLoc[1] + " " + next + " " + " place");

    }

    public static void remove(int[] nextLoc, int next){
        locationGrid[nextLoc[0]][nextLoc[1]] = 0;

        valueTable[next-1][0] = -1;
        valueTable[next-1][1] = -1;

        g.clearCell(nextLoc[0], nextLoc[1]);
//        System.out.println(nextLoc[0] + " " + nextLoc[1] + " " + next+ " remove");
    }

}
