import org.nlogo.api.*;

public class
MathExtension extends DefaultClassManager
{
	// Coefficients in rational approximations.
	private static final double ICDF_A[] = {
		-3.969683028665376e+01,  2.209460984245205e+02,
		-2.759285104469687e+02,  1.383577518672690e+02,
		-3.066479806614716e+01,  2.506628277459239e+00
	};

	private static final double ICDF_B[] = {
		-5.447609879822406e+01,  1.615858368580409e+02,
		-1.556989798598866e+02,  6.680131188771972e+01,
		-1.328068155288572e+01
	};

	private static final double ICDF_C[] = {
		-7.784894002430293e-03, -3.223964580411365e-01,
		-2.400758277161838e+00, -2.549732539343734e+00,
		4.374664141464968e+00,  2.938163982698783e+00
	};

	private static final double ICDF_D[] = {
		7.784695709041462e-03,  3.224671290700398e-01,
		2.445134137142996e+00,  3.754408661907416e+00
	};
	private static double
	doubleval(Argument a) throws ExtensionException
	{
		try{
			return a.getDoubleValue();
		}catch(LogoException e){
			throw new ExtensionException(e.getMessage());
		}
	}
	public void
	load(PrimitiveManager m)
	{
		m.addPrimitive("inverse-normal", new Invnormal());
		m.addPrimitive("erf", new ErrorFunction());
	}

	private static void
	print(Object o)
	{
		System.out.println(o);
	}

	static public class
	Invnormal extends DefaultReporter
	{
		public Syntax
		getSyntax()
		{
			return Syntax.reporterSyntax(
				new int[] {
					Syntax.TYPE_NUMBER
				}, Syntax.TYPE_NUMBER);
		}

		public Object
		report(Argument a[], Context context)
			throws ExtensionException
		{
			return new Double(Fns.inversenormal(doubleval(a[0])));
		}
	}

	static public class
	ErrorFunction extends DefaultReporter
	{
		public Syntax
		getSyntax()
		{
			return Syntax.reporterSyntax(
				new int[] {
					Syntax.TYPE_NUMBER
				}, Syntax.TYPE_NUMBER);
		}

		public Object
		report(Argument a[], Context context)
			throws ExtensionException
		{
			return new Double(Fns.erf(doubleval(a[0])));
		}
	}

	static public class
	Fns
	{
		/*
		 * Original algorithm and Perl implementation can be found at:
		 * http://www.math.uio.no/~jacklam/notes/invnorm/index.html
		 * Author:
		 *  Peter J. Acklam
		 *  jacklam@math.uio.no
		 */
		private static final double P_LOW  = 0.02425D;
		private static final double P_HIGH = 1.0D - P_LOW;
	
		public static double
		inversenormal(double d)
		{
			double z = 0;
	
			if(d == 0)
				z = Double.NEGATIVE_INFINITY;
			else if(d == 1)
				z = Double.POSITIVE_INFINITY;
			else if(Double.isNaN(d) || d < 0 || d > 1)
				z = Double.NaN;
			else if( d < P_LOW ){
				// Rational approximation for lower region:
				double q  = Math.sqrt(-2*Math.log(d));
				z = (((((ICDF_C[0]*q+ICDF_C[1])*q+ICDF_C[2])*q+ICDF_C[3])*q+ICDF_C[4])*q+ICDF_C[5]) / ((((ICDF_D[0]*q+ICDF_D[1])*q+ICDF_D[2])*q+ICDF_D[3])*q+1);
			}
			else if ( P_HIGH < d ){
				// Rational approximation for upper region:
				double q  = Math.sqrt(-2*Math.log(1-d));
				z = -(((((ICDF_C[0]*q+ICDF_C[1])*q+ICDF_C[2])*q+ICDF_C[3])*q+ICDF_C[4])*q+ICDF_C[5]) / ((((ICDF_D[0]*q+ICDF_D[1])*q+ICDF_D[2])*q+ICDF_D[3])*q+1);
			}
			else{
		 		// Rational approximation for central region:
				double q = d - 0.5D;
				double r = q * q;
				z = (((((ICDF_A[0]*r+ICDF_A[1])*r+ICDF_A[2])*r+ICDF_A[3])*r+ICDF_A[4])*r+ICDF_A[5])*q / (((((ICDF_B[0]*r+ICDF_B[1])*r+ICDF_B[2])*r+ICDF_B[3])*r+ICDF_B[4])*r+1);
			}
			return z;
		}

		// http://www.cs.princeton.edu/introcs/21function/ErrorFunction.java.html
		// Implements the Gauss error function.
		//	erf(z) = 2 / sqrt(pi) * integral(exp(-t*t), t = 0..z)
		//
		// fractional error in math formula less than 1.2 * 10 ^ -7.
		// although subject to catastrophic cancellation when z in very close to 0
		// from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
		public static double
		erf(double z)
		{
			double t;
			t = 1.0 / (1.0 + 0.5 * Math.abs(z));
	
			// use Horner's method
			double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
						t * ( 1.00002368 +
						t * ( 0.37409196 + 
						t * ( 0.09678418 + 
						t * (-0.18628806 + 
						t * ( 0.27886807 + 
						t * (-1.13520398 + 
						t * ( 1.48851587 + 
						t * (-0.82215223 + 
						t * ( 0.17087277))))))))));
			if (z >= 0)
				return  ans;
			else
				return -ans;
		}
	
		// fractional error less than x.xx * 10 ^ -4.
		// Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
		public static double
		erf2(double z)
		{
			double t, poly, ans;
	
			t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
			poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
			ans = 1.0 - poly * Math.exp(-z*z);
			if (z >= 0)
				return  ans;
			else
				return -ans;
		}
	
		// cumulative normal distribution
		// See Gaussia.java for a better way to compute Phi(z)
		public static double Phi(double z)
		{
			return 0.5 * (1.0 + erf(z / (Math.sqrt(2.0))));
		}
	}

}
