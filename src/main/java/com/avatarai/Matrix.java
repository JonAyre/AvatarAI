package com.avatarai;

import java.util.Objects;

public class Matrix {
    private final double[][] matrix;

    public Matrix(double[][] other) {
        matrix = new double[other.length][other[0].length];
        for (int i = 0; i < other.length; i++) {
            for (int j = 0; j < other[i].length; j++) {
                matrix[i][j] = other[i][j];
            }
        }
    }

    public Matrix(int rows, int columns) {
        matrix = new double[rows][columns];
    }

    public Matrix add(Matrix other) {
        if (this.matrix.length != other.matrix.length && this.matrix[0].length != other.matrix[0].length) {
            return null;
        }

        Matrix newMatrix = new Matrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] += other.matrix[row][col];
            }
        }

        return newMatrix;
    }

    public Matrix subtract(Matrix other) {
        Matrix newMatrix = other.scale(-1);
        return newMatrix.add(this);
    }

    public Matrix multiply(Matrix other) {
        if (this.matrix[0].length != other.matrix.length && this.matrix.length != other.matrix[0].length) {return null;}

        Matrix newMatrix = new Matrix(this.matrix.length, other.matrix[0].length);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < other.matrix[0].length; col++) {
                for (int cell = 0; cell < this.matrix[row].length; cell++) {
                    newMatrix.matrix[row][col] += this.matrix[row][cell] * other.matrix[cell][col];
                }
            }
        }

        return newMatrix;
    }

    public Matrix transpose() {
        Matrix newMatrix = new Matrix(this.matrix[0].length, this.matrix.length);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[col][row] = this.matrix[row][col];
            }
        }
        return newMatrix;
    }

    public Matrix scale(double scale) {
        Matrix newMatrix = new Matrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = this.matrix[row][col] * scale;
            }
        }
        return newMatrix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Matrix matrix1 = (Matrix) o;
        return Objects.deepEquals(matrix, matrix1.matrix);
    }
}
