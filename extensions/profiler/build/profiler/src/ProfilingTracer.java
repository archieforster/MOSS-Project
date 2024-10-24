/** (c) 2007 Uri Wilensky. See README.txt for terms of use. **/

package org.nlogo.extensions.profiler ;

import org.nlogo.nvm.Activation ;
import org.nlogo.nvm.Context ;
import java.util.Hashtable ;
import java.util.Vector ;
import java.util.TreeSet ;
import java.util.Map ;
import java.util.Formatter ;
import java.util.Comparator ;


public class ProfilingTracer extends org.nlogo.nvm.Tracer
{
	public boolean enabled = false ;
	public Hashtable<String, Long> inclusiveTimes ;
    public Hashtable<String, Long> exclusiveTimes ;
	public Hashtable<String, Long> callCounts ;

    private Hashtable<Activation, CallRecord> openRecords ;
    private Vector callRoots ;

    public ProfilingTracer()
    {
		super() ;
		reset() ;
    }

    public void openCallRecord( Context context, Activation activation )
    {

		if ( ! enabled ) return;

		long startTime = System.nanoTime() ;
		ProcedureCallRecord record = new ProcedureCallRecord( activation.procedure,
															  context.agent.toString(),
															  null ) ;
		record.startTime = startTime ;

		// find parent call
		ProcedureCallRecord parent = ( ProcedureCallRecord )openRecords.get( activation.parent ) ;
		if ( parent == null )
	    {
			// we don't keep call tree data around right now
			//callRoots.add( record ) ;
	    }
		else
	    {
			record.caller = parent ;
			parent.called.add( record ) ;
	    }
		openRecords.put( activation, record ) ;
    }
	
    public void closeCallRecord( Context context, Activation activation )
    {

		if ( ! enabled ) return;
		
		long stopTime = System.nanoTime() ;
		ProcedureCallRecord record = ( ProcedureCallRecord )openRecords.get( activation ) ;
		
		if ( record == null ) {
			if ( Boolean.getBoolean( "org.nlogo.profiler.verbose" ) ) { 
				System.err.println("Cannot find record for: " + activation.procedure.name) ;
			}
			// return if we can't find the record
			return ;
		}
		
		record.stopTime = stopTime ;
		updateProcedureTimingData( record ) ;

		openRecords.remove( activation ) ;
		
		if ( Boolean.getBoolean( "org.nlogo.profiler.verbose" ) )
			System.out.println( record ) ;
    }

    protected void updateProcedureTimingData( CallRecord record )
    {

		Long callCount = (Long) callCounts.get( record.name ) ;
		if ( callCount == null ) { callCount = new Long( (long) 0 ) ; }
		callCounts.put( record.name,
						new Long( callCount.longValue() + 1 ) ) ;
		
		Long inclusiveTime = (Long) inclusiveTimes.get( record.name ) ;
		if ( inclusiveTime == null ) { inclusiveTime = new Long( (long) 0 ) ; }
		inclusiveTimes.put( record.name,
							new Long( inclusiveTime.longValue() + record.inclusiveTime() ) ) ;
		
		Long exclusiveTime = (Long) exclusiveTimes.get( record.name ) ;
		if ( exclusiveTime == null ) { exclusiveTime = new Long( (long) 0 ) ; }
		exclusiveTimes.put( record.name,
							new Long( exclusiveTime.longValue() + record.exclusiveTime() ) ) ;
    }

    public void reset()
    {
		openRecords = new Hashtable<Activation,CallRecord>() ;
		inclusiveTimes = new Hashtable<String, Long>() ;
		exclusiveTimes = new Hashtable<String, Long>() ;
		callCounts = new Hashtable<String, Long>() ;
		callRoots = new Vector() ;
    }


    public void dump(java.io.PrintStream stream)
    {
		stream.println( "BEGIN PROFILING DUMP" ) ;
		stream.println( "Sorted by Exclusive Time" ) ;
		dumpSortedBy(exclusiveTimes, stream);
		stream.println("");
		stream.println( "Sorted by Inclusive Time" ) ;
		dumpSortedBy(inclusiveTimes, stream);
		stream.println("");
		stream.println( "Sorted by Number of Calls" ) ;
		dumpSortedBy(callCounts, stream);
		stream.println( "END PROFILING DUMP" ) ;
    }
	
    public void dumpSortedBy(Hashtable<String,Long> sortOn, java.io.PrintStream stream)
    {
		TreeSet<Map.Entry> sortedSet = new TreeSet<Map.Entry>( new DescendingMapEntryComparator() ) ;
		sortedSet.addAll( sortOn.entrySet() ) ;
		
		stream.format( "%-30s%10s %10s %10s %10s\n",
					   "Name", "Calls", "Incl T(ms)", "Excl T(ms)", "Excl/calls" ) ;
		
		java.util.Iterator it = sortedSet.iterator();
		while( it.hasNext() )
			this.dumpProcedure((String)((Map.Entry)it.next()).getKey(), stream);
		
    }
	
    void dumpProcedure(String name, java.io.PrintStream stream)
    {
		long calls = ( ( Long ) callCounts.get( name ) ).longValue() ;
		double itime = ( ( Long ) inclusiveTimes.get( name ) ).doubleValue() ;
		double etime = ( ( Long ) exclusiveTimes.get( name ) ).doubleValue() ;
		stream.format( "%-30s%10d %10.3f %10.3f %10.3f\n",
					   name,
					   calls,
					   new Double( itime / 1000000.0 ),
					   new Double( etime / 1000000.0 ),
					   new Double( ( etime / calls ) / 1000000.0 ) ) ;
	
    }
	
    class DescendingMapEntryComparator implements Comparator<Map.Entry>
    {
		public int compare(Map.Entry e1, Map.Entry e2)
		{
			Long val1 = (Long) e1.getValue() ;
			Long val2 = (Long) e2.getValue() ;
			
			return val2.compareTo(val1) ;
		}
		
    }
	
    public void enable() { this.enabled = true ;}
    public void disable() { this.enabled = false ; }
	
}
