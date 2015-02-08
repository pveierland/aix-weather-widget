package net.veierland.aix.util;

public class Cubic {
	
	public static class CubicResult {
		
		public double[] roots;
		
		public CubicResult(double... args) {
			roots = args;
		}
		
	}
	
	/* Finds the real roots of a cubic equation. Works for polynomials of order 1 to 3 */
	/* Returns: The number of real roots found. -1 if the input was erroneous */
	public static CubicResult solveReal(double a, double b, double c, double d) {
		if (a != 0.0) {
			// Equation is cubic
			double A = b / a;
			double B = c / a;
			double C = d / a;
			
			double Q = (3.0 * B - Math.pow(A, 2.0)) / 9.0;
			double R = (9.0 * A * B - 27.0 * C - 2.0 * Math.pow(A, 3.0)) / 54.0;
			double D = Math.pow(Q, 3.0) + Math.pow(R, 2.0);
			
			if (D > 0.0) {
				// If D is greater than zero, there is one real and two complex roots
				double S = Math.cbrt(R + Math.sqrt(D));
				double T = Math.cbrt(R - Math.sqrt(D));
				
				double x1 = S + T - A / 3.0;
				
				return new CubicResult(x1);
			} else if (D == 0.0) {
				// If D is equal to zero, there are three real roots
				// At least two of the roots are equal
				
				double x1 = 2.0 * Math.cbrt(R) - A / 3.0;
				double x2 = -Math.cbrt(R) - A / 3.0;
				double x3 = x2;
				
				return new CubicResult(x1, x2, x3);
			} else {
				// If D is less than zero, all roots are real and unequal
				double theta = Math.acos(R / Math.sqrt(Math.pow(-Q, 3.0)));
				
				double x1 = 2.0 * Math.sqrt(-Q) * Math.cos(theta / 3.0) - A / 3.0;
				double x2 = 2.0 * Math.sqrt(-Q) * Math.cos((theta + 2.0 * Math.PI) / 3.0) - A / 3.0;
				double x3 = 2.0 * Math.sqrt(-Q) * Math.cos((theta + 4.0 * Math.PI) / 3.0) - A / 3.0;
				
				return new CubicResult(x1, x2, x3);
			}
		} else if (b != 0.0) {
			// Equation is quadratic
			double discriminant = Math.pow(c, 2.0) - 4.0 * b * d;
			if (discriminant > 0.0) {
				// There are two distinct real roots
				double x1 = (-c + Math.sqrt(discriminant)) / (2.0 * b);
				double x2 = (-c - Math.sqrt(discriminant)) / (2.0 * b);
				
				return new CubicResult(x1, x2);
			} else if (discriminant == 0.0) {
				// There is a single real root
				double x1 = -c / (2.0 * b);
				
				return new CubicResult(x1);
			} else {
				// There are no real roots
				return new CubicResult();
			}
		} else if (c != 0.0) {
			// Equation is a polynomial of first degree
			// There is one real root
			double x1 = -d / c;
			
			return new CubicResult(x1);
		} else {
			// Invalid input
			return null;
		}
	}
	
}
