package com.avatarai;

import java.util.Objects;
import java.util.function.Function;

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

    public Matrix(Matrix other) {
        matrix = new double[other.matrix.length][other.matrix[0].length];
        for (int i = 0; i < other.matrix.length; i++) {
            for (int j = 0; j < other.matrix[i].length; j++) {
                matrix[i][j] = other.matrix[i][j];
            }
        }
    }

    public Matrix(int rows, int columns) {
        matrix = new double[rows][columns];
    }

    public static Matrix randomMatrix(int rows, int columns, double min, double max) {
        Matrix newMatrix = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                newMatrix.matrix[i][j] = min + Math.random() * (max - min);
            }
        }
        return newMatrix;
    }

    public int rows() {
        return matrix.length;
    }

    public int columns() {
        return matrix[0].length;
    }

    public void setValue(int row, int column, double value) {
        matrix[row][column] = value;
    }

    public double getValue(int row, int column) {
        return matrix[row][column];
    }

    public double[] column(int column) {
        double[] columnValues = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            columnValues[i] = matrix[i][column];
        }
        return columnValues;
    }

    public double[] row(int row) {
        return matrix[row];
    }

    public static Matrix identityMatrix(int size) {
        Matrix identity = new Matrix(size, size);
        for (int i = 0; i < size; i++) {
            identity.matrix[i][i] = 1;
        }
        return identity;
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

    public Matrix applyFunction(Function<Double, Double> function) {
        Matrix newMatrix = new Matrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = function.apply(this.matrix[row][col]);
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
