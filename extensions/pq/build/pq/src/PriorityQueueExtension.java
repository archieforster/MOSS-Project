// This NetLogo extension provides a priority queue data type,
// based on a simple binary heap algorithm.

package org.nlogo.extensions.pq;

import org.nlogo.api.LogoException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.Argument;
import org.nlogo.api.Syntax;
import org.nlogo.api.Context;
import org.nlogo.api.LogoList;
import org.nlogo.api.Reporter;
import org.nlogo.api.Command;

public class
PriorityQueueExtension
	extends org.nlogo.api.DefaultClassManager
{
	private static boolean Checks = false;

	private static class
	Element {
		double r;
		Object obj;
	}

	private static final int PQUEUE = Syntax.TYPE_WILDCARD;
	private static final int ANY = Syntax.TYPE_WILDCARD;
	private static final int NUM = Syntax.TYPE_NUMBER;

	public void
	load(org.nlogo.api.PrimitiveManager m)
	{
		m.addPrimitive("new", (new New()).withSyntax(PQUEUE, new int[]{}));
		m.addPrimitive("add", (new Add()).withSyntax(new int[]{PQUEUE, NUM, ANY}));
		m.addPrimitive("remove", (new Remove()).withSyntax(ANY, new int[]{PQUEUE}));
		m.addPrimitive("delete-item", (new DeleteItem()).withSyntax(new int[]{PQUEUE, ANY}));
		m.addPrimitive("peek", (new Peek()).withSyntax(ANY, new int[]{PQUEUE}));
		m.addPrimitive("length", (new Length()).withSyntax(NUM, new int[]{PQUEUE}));
	}

	public org.nlogo.api.ExtensionObject
	readExtensionObject(org.nlogo.api.ExtensionManager em, String typeName, String value)
		throws ExtensionException
	{
		throw new ExtensionException("cannot yet read pq");
	}

	// pq:new
	public static class
	New extends PQReporter
	{
		public Object
		report(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			return new PQ(10);
		}
	}

	// pq:add q rank value
	public static class
	Add extends PQCommand
	{
		public void
		perform(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			getpq(args[0]).add(args[1].getDoubleValue(), args[2].get());
		}			      
	}

	// pq:remove q
	public static class
	Remove extends PQReporter
	{
		public Object
		report(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			return getpq(args[0]).remove();
		}					      
	}

	// pq:remove q
	public static class
	Peek extends PQReporter
	{
		public Object
		report(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			return getpq(args[0]).peek();
		}	      
	}
	
	// pq:delete-item q value
	public static class
	DeleteItem extends PQCommand
	{
		public void
		perform(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			getpq(args[0]).delitem(args[1].get());
		}
	}
	
	// pq:length q
	public static class
	Length extends PQReporter
	{
		public Object
		report(Argument args[], Context context)
			throws ExtensionException, LogoException
		{
			return new Double(getpq(args[0]).n);
		}
	}
	
	private static PQ
	getpq(Argument arg) throws ExtensionException, LogoException
	{
		Object obj = arg.get();
		if(!(obj instanceof PQ))
			error("not a priority queue: "+org.nlogo.agent.Dump.logoObject(arg));
		return (PQ)obj;
	}
	
	private static void
	error(String s) throws ExtensionException
	{
		throw new ExtensionException(s);
	}

	// the actual priority queue class.
	private static class
	PQ implements org.nlogo.api.ExtensionObject
	{
		int n;
		Element a[];
		
		PQ(int size)
		{
			n = 0;
			a = new Element[size];
			for(int i = 1; i < size; i++)
				a[i] = new Element();
		}
		
		public void
		add(double rank, Object v) throws ExtensionException
		{
			++n;
			if(n >= a.length){
				Element na[] = new Element[n * 2];
				System.arraycopy(a, 0, na, 0, a.length);
				a = na;
				for(int i = na.length - 1; i >= n; i--)
					na[i] = new Element();
			}
			a[n].obj = v;
			a[n].r = rank;
			this.upheap(n);
			if(Checks)
				this.check();
		}
		
		public Object
		remove() throws ExtensionException
		{
                             if(n < 1)
                             	error("pq:remove on empty queue");
                             Object v = a[1].obj;
                             move(1, n);
                             a[n].obj = null;
                             n--;
                             this.downheap(1);
                             if(Checks)
                             	this.check();
                             return v;
		}

		public Object
		peek() throws ExtensionException
		{
			if(n < 1)
				error("pq:peek on empty queue");
			return a[1].obj;
		}
		
		public void
		delitem(Object v) throws ExtensionException
		{
			for(int i = 1; i <= n; i++){
				if(a[i].obj.equals(v)){
					move(i, n);
					a[n].obj = null;
					n--;
					if(i-1 != n){
						if(!this.downheap(i))
							this.upheap(i);
					}
					if(Checks)
						this.check();
					return;
				}
			}
			error("pq:delitem not found");
		}
		
		private void
		move(int i0, int i1)
		{
			a[i0].r = a[i1].r;
			a[i0].obj = a[i1].obj;
		}
		
		private boolean
		upheap(int k)
		{
			int i = k;
			double vr = a[k].r;
			Object vobj = a[k].obj;
			for(;;){
				int m = k >> 1;
				if(m == 0 || vr <= a[m].r)
					break;
				move(k, m);
				k = m;
			}
			a[k].r = vr;
			a[k].obj = vobj;
			return i != k;
		}
		
		private boolean
		downheap(int k)
		{
			int i = k;
			double vr = a[k].r;
			Object vobj = a[k].obj;
			int halfn = n >> 1;
			while(k <= halfn){
				int j = k+k;
				if(j < n && a[j+1].r > a[j].r)
					j++;
				if(a[j].r <= vr)
					break;
				move(k, j);
				k = j;
			}
			a[k].r = vr;
			a[k].obj = vobj;
			return i != k;
		}

		public String
		dump(boolean readable, boolean exporting)
		{
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < n; i++){
				Element e = a[i];
				buf.append(" [");
				buf.append(Double.toString(e.r));
				buf.append(" ");
				buf.append(org.nlogo.agent.Dump.logoObject(e.obj, true, true));
				buf.append("]");
			}
			return buf.toString() ;
		}

		public String
		getExtensionName()
		{
			return "pq";
		}

		public String
		getNLTypeName()
		{
			return "";
		}

		public boolean
		recursivelyEqual(Object o)
		{
			// could duplicate heap and remove all elements in order, comparing along the way,
			// but leave it for the time being.
			return o == this;
		}

		// debugging		
		private String
		info()
		{
			String s = "n: "+n+"; len: "+a.length+"[";
			for(int i = 1; i <= n; i++){
				if(a[i] == null)
					s = s + "null ";
				else
					s = s + "("+a[i].r+", "+a[i].obj+") ";
			}
			return s + "]";
		}

		// debugging
		private void
		check() throws ExtensionException
		{
			for(int i = 1; i <= n; i++){
				int j = i + i;
				if(j <= n && a[j].r > a[i].r)
					error("pq: violation at "+i+" < "+j+"; " + info());
				j++;
				if(j <= n && a[j].r > a[i].r)
					error("pq: violation at "+i+" < "+j+"; " + info());
			}
		}
	}

	// semi-abstract implementation of Reporter to allow compact implementation of reporter classes above.
	private static abstract class
	PQReporter extends Object implements Reporter
	{
		int args[];
		int ret;

		public Reporter
		withSyntax(int ret, int args[])
		{
			this.args = args;
			this.ret = ret;
			return this;
		}
		public Syntax
		getSyntax()
		{
			return Syntax.reporterSyntax(args, ret);
		}
		public String
		getAgentClassString()
		{
			return "OTPL";
		}
		public Reporter
		newInstance(String s)
		{
			try {
				return ((PQReporter)(this.getClass().getConstructor(null).newInstance(null))).withSyntax(ret, args);
			} catch (Exception e) {
				throw new java.lang.RuntimeException("cannot create new instance of "+this.getClass());
			}
		}
		public abstract Object
		report(Argument args[], Context context)
			throws ExtensionException, LogoException;
	}
	
	// semi-abstract implementation of Command to allow compact implementation of command classes above.
	private static abstract class
	PQCommand extends Object implements Command
	{
		int args[];
		public Command
		withSyntax(int args[])
		{
			this.args = args;
			return this;
		}

		public Syntax
		getSyntax()
		{
			return Syntax.commandSyntax(args);
		}
		public String
		getAgentClassString()
		{
			return "OTPL";
		}
		public boolean
		getSwitchesBoolean()
		{
			return false;
		}
		public Command
		newInstance(String s)
		{
			try {
				return ((PQCommand)(this.getClass().getConstructor(null).newInstance(null))).withSyntax(args);
			} catch (Exception e) {
				throw new java.lang.RuntimeException("cannot create new instance of "+this.getClass() + ": " + e);
			}
		}
	}
}
