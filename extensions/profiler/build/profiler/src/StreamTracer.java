/** (c) 2007 Uri Wilensky. See README.txt for terms of use. **/

package org.nlogo.extensions.profiler ;

import org.nlogo.nvm.Activation ;
import org.nlogo.nvm.Context ;

public class StreamTracer extends org.nlogo.nvm.Tracer
{
	java.io.PrintStream out ;

	public StreamTracer()
	{
		this(System.out);
	}

	public StreamTracer( java.io.PrintStream out )
	{
		super();
		this.out = out ;
	}
	
	public void openCallRecord( Context context, Activation activation )
	{
		out.println("START " + System.nanoTime() + " - " +
					context.job + "/" + context + "/" + activation.hashCode() + "/(" +
					context.agent.toString() + ") - " + activation.procedure.name);
	}

	public void closeCallRecord( Context context, Activation activation )
	{
		out.println("STOP " + System.nanoTime() + " - " +
					context.job + "/" + context + "/" + activation.hashCode() + "/(" +
					context.agent.toString() + ") - " + activation.procedure.name);
	}

	public void closeCurrentCallRecord( int token )
	{
		//out.println(" " + record.toString() ) ;
	}

	public void enable() {}
	public void disable() {}
	public void reset() {}
	public void dump(java.io.PrintStream stream) {}
}
