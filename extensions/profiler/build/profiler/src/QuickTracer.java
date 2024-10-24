package org.nlogo.extensions.profiler;
// somewhat faster profiling. © roger peppe, july 2008.
import java.util.HashMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Arrays;
import org.nlogo.nvm.Activation;
import org.nlogo.nvm.Context;
import org.nlogo.command.Procedure;
import org.nlogo.api.LogoList;

public class QuickTracer extends org.nlogo.nvm.Tracer
{
	private static class Record  {
		Procedure procedure;	// procedure that this record refers to.
		int running;			// number of currently running instances.
		long inclusive;			// accumulated inclusive time.
		long exclusive;			// accumulated exclusive time.
		long invoketime;		// when first currently running instance invoked.
		long callcount;			// number of calls.
	}
	private boolean enabled;
	private HashMap<Procedure, Record> records;
	private Record current;		// currently executing procedure.
	private Activation frame;		// current activation
	private long resumetime;		// when current procedure resumed.

	public QuickTracer()
	{
		super();
		reset();
	}
	
	private Record
	newproc(Procedure p, long t)
	{
		Record r = new Record();
		r.procedure = p;
		r.inclusive = 0;
		r.exclusive = 0;
		r.running = 1;
		r.invoketime = t;
		r.callcount = 0;
		this.records.put(p, r);
		return r;
	}

	public void reset()
	{
		this.records = new HashMap<Procedure, Record>(100);
		this.current = null;
		this.frame = null;
		this.enabled = false;
		this.newproc(null, 0L);		// add entry for outermost level
	}

	public void enable()
	{
		this.current = this.records.get(null);
		this.frame = null;
		long t = System.nanoTime();
		this.resumetime = t;
		this.current.invoketime = t;
		enabled = true;
	}

	public void disable()
	{
		// close calls for all procedures in the stack,
		// as we can't keep track from now on.
		if(this.enabled){
			Record toplev = this.records.get(null);
			while(this.current != toplev)
				this.closeCallRecord(null, this.frame);
			long t = System.nanoTime();
			toplev.exclusive += t - this.resumetime;
			toplev.inclusive += t - toplev.invoketime;
			this.enabled = false;
		}
	}

	public void openCallRecord(Context context, Activation activation)
	{
		if(enabled){
			long t = System.nanoTime();
			Procedure p = activation.procedure;
			Record r = this.records.get(p);
			if(r == null)
				r = newproc(p, t);
			else if(r.running++ == 0)
				r.invoketime = t;
			this.current.exclusive += t - this.resumetime;
			r.callcount++;
			this.current = r;
			this.frame = activation;
			this.resumetime = t;
		}
	}
	
	public void closeCallRecord(Context context, Activation activation)
	{
		if(enabled){
			long t = System.nanoTime();
			Record r = this.records.get(activation.procedure);
			// we can get close without open if profiler is started within a procedure.
			if(r != null){
				if(--r.running == 0)
					r.inclusive += t - r.invoketime;
				this.current.exclusive += t - this.resumetime;
				this.frame = activation.parent;
				this.current = this.records.get(this.frame.procedure);
				// if we're ascending to a procedure we didn't start in,
				// substitute our own top-level instead.
				if(this.current == null || this.current.running == 0){
					this.current = this.records.get(null);
					this.frame = null;
				}
				this.resumetime = t;
			}
		}
	}

	public static class Recordcmp implements Comparator<Record>
	{
		public int compare(Record r1, Record r2)
		{
			return -(new Long(r1.exclusive).compareTo(new Long(r2.exclusive)));
		}
	}

	public void dump(java.io.PrintStream s)
	{
		// remove the root record so it doesn't show up in the results.
		Record toplev = this.records.remove(null);
		Record[] recs = this.records.values().toArray(new Record[0]);
		java.util.Arrays.sort(recs, new Recordcmp());
		double total = toplev.inclusive;
		if(total == 0)
			total = 1.0;
		s.format("total time(seconds): %.2f\n", (double)toplev.inclusive / 1e9);
		s.format("unaccounted time(seconds): %.4f\n", (double)toplev.exclusive / 1e9);
		s.format("%5s %8s %10s %8s %11s %13s %s\n", "%", "self", "all", "", "self", "all", "");
		s.format("%5s %8s %10s %8s %11s %13s %s\n", "time", "seconds", "seconds", "calls", "us/call", "us/call", "name");
		for(int i = 0; i < recs.length; i++){
			Record r = recs[i];
			s.format("%5.1f %8.2f %10.2f %8d %11.2f %13.2f %s\n",
				(double)r.exclusive / total * 100.0,
				(double)r.exclusive / 1e9,
				(double)r.inclusive / 1e9,
				r.callcount,
				(double)r.exclusive / (double)r.callcount / 1e3,
				(double)r.inclusive / (double)r.callcount / 1e3,
				r.procedure.name);
		}
		this.records.put(null, toplev);
	}

	public Object values()
	{
		Record toplev = this.records.remove(null);
		Record[] recs = this.records.values().toArray(new Record[0]);
		java.util.Arrays.sort(recs, new Recordcmp());
		double total = toplev.inclusive;
		if(total == 0)
			total = 1.0;
		LogoList l = new LogoList();
		for(int i = recs.length - 1; i >= 0; i--){
			Record r = recs[i];
			l = l.fput(new LogoList(Arrays.asList(
				new Double(r.exclusive / total * 100.0),
				new Double((double)r.exclusive / 1e9),
				new Double((double)r.inclusive / 1e9),
				new Double((double)r.callcount / 1e9),
				new Double((double)r.exclusive / (double)r.callcount / 1e9),
				new Double((double)r.inclusive / (double)r.callcount / 1e9),
				r.procedure.name)));
		}
		l = l.fput(new Double(toplev.exclusive));
		l = l.fput(new Double(toplev.inclusive));
		this.records.put(null, toplev);
		return l;
	}
}
