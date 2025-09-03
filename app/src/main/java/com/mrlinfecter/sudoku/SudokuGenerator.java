package com.mrlinfecter.sudoku;

import java.util.Random;

public class SudokuGenerator {
    private static final int SIZE = 9;

    public int[][] generateSolution() {
        int[][] grid = new int[SIZE][SIZE];
        fillGrid(grid);
        return grid;
    }

    public int[][] generatePuzzle(int[][] solution, int emptyCells) {
        int[][] puzzle = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            puzzle[i] = solution[i].clone();
        }
        Random rand = new Random();
        int count = emptyCells;
        while (count > 0) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (puzzle[r][c] != 0) {
                puzzle[r][c] = 0;
                count--;
            }
        }
        return puzzle;
    }

    private boolean fillGrid(int[][] grid) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == 0) {
                    int[] numbers = shuffledNumbers();
                    for (int num : numbers) {
                        if (isSafe(grid, row, col, num)) {
                            grid[row][col] = num;
                            if (fillGrid(grid)) return true;
                            grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private int[] shuffledNumbers() {
        int[] numbers = {1,2,3,4,5,6,7,8,9};
        Random rand = new Random();
        for (int i = 0; i < numbers.length; i++) {
            int j = rand.nextInt(numbers.length);
            int tmp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = tmp;
        }
        return numbers;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == num || grid[i][col] == num) return false;
        }
        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[startRow + i][startCol + j] == num) return false;
            }
        }
        return true;
    }
}
