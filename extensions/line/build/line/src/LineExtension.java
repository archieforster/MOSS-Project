import org.nlogo.api.*;

public class
LineExtension extends DefaultClassManager
{
	public void
	load(PrimitiveManager m)
	{
		m.addPrimitive("line", new Linecover());
	}

	private static void
	print(Object o)
	{
		System.out.println(o);
	}

	static public class
	Extutils
	{
		public static Object
		runReporter(Argument a, Context context)
			throws ExtensionException
		{
			org.nlogo.command.Reporter r;
			org.nlogo.nvm.Context nvmctxt;
			r = ((org.nlogo.nvm.Argument)a).getReporter();
			nvmctxt = ((org.nlogo.nvm.ExtensionContext)context).nvmContext();
			try{
				return r.report(nvmctxt);
			}catch (LogoException e){
				throw new ExtensionException(e.getMessage());
			}
		}
	}
	static public class
	Linecover extends DefaultReporter
	{

		public Syntax
		getSyntax()
		{
			return Syntax.reporterSyntax(
				new int[] {
					Syntax.TYPE_NUMBER,
					Syntax.TYPE_NUMBER,
					Syntax.TYPE_NUMBER,
					Syntax.TYPE_NUMBER
				}, Syntax.TYPE_LIST);
		}
		
		private static int
		intval(Argument a) throws ExtensionException
		{
			try{
				return a.getIntValue();
			}catch(LogoException e){
				throw new ExtensionException(e.getMessage());
			}
		}


		public Object
		report(Argument a[], Context context)
			throws ExtensionException
		{
			return bresline(intval(a[0]), intval(a[1]), intval(a[2]), intval(a[3]));
		}

		static private LogoList
		bresline(double x0, double y0, double x1, double y1)
		{
			double t;
			boolean rev = false;
			boolean steep = rabs(y1 - y0) > rabs(x1 - x0);
			if(steep){
				t = x0;
				x0 = y0;
				y0 = t;
				
				t = x1;
				x1 = y1;
				y1 = t;
			}
			if(x0 > x1){
				t = x0;
				x0 = x1;
				x1 = t;
				
				t = y0;
				y0 = y1;
				y1 = t;

				rev = true;
			}
			LogoList l = new LogoList();
			bresline0(steep, (int)Math.round(x0), (int)Math.round(y0), (int)Math.round(x1), (int)Math.round(y1), l);
			if(rev)
				l = l.reverse();
			return l;
		}

		// line running from left to right, subtending < 45deg from horizontal
		static private void
		bresline0(boolean r, int x0, int y0, int x1, int y1, LogoList l)
		{
			int dx = x1 - x0;
			int dy = abs(y1 - y0);
			int error = -(dx + 1) / 2;
			
			int y = y0;
			int ystep;
			if(y0 < y1)
				ystep = 1;
			else
				ystep = -1;
			for(int x = x0; x <= x1; x++){
				rplot(r, x, y, l);
				error += dy;
				if(error >= 0){
					y += ystep;
					error -= dx;
				}
			}
		}
		
		static private void
		rplot(boolean r, int x, int y, LogoList l)
		{
			if(r){
				int t = x;
				x = y;
				y = t;
			}
			LogoList p = new LogoList();
			p.add(new Double(x));
			p.add(new Double(y));
			l.add(p);
		}
		static private double
		rabs(double x)
		{
			if(x >= 0)
				return x;
			return -x;
		}
		static private int
		abs(int x)
		{
			if(x >= 0)
				return x;
			return -x;
		}
	}
}
