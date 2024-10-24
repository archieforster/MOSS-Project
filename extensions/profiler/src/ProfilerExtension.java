/** (c) 2007 Uri Wilensky. See README.txt for terms of use. **/

package org.nlogo.extensions.profiler;

import org.nlogo.api.Syntax;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Argument; 
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.Tracer;

public class ProfilerExtension extends org.nlogo.api.DefaultClassManager
{
	static String defaultClassName = "org.nlogo.extensions.profiler.QuickTracer" ;
	private static Tracer tracer;

	public void load( org.nlogo.api.PrimitiveManager primManager )
	{
		primManager.addPrimitive( "start", new ProfilerStart());
		primManager.addPrimitive( "stop", new ProfilerStop());
		primManager.addPrimitive( "reset", new ProfilerReset());
		primManager.addPrimitive( "report", new ProfilerReport());
		primManager.addPrimitive( "report-values", new ProfilerValues());
	
		// For changing which profiler class is used
		primManager.addPrimitive( "set-class", new ProfilerClass());
	}
 		
	public void runOnce( org.nlogo.api.ExtensionManager em ) throws ExtensionException
		
	{
		loadTracer(defaultClassName);
		Tracer.profilingTracer = tracer;
	}

	public static class ProfilerStart extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax() ;
		}
		public void perform(Argument args[], Context context) throws ExtensionException
		{

			if ( Boolean.getBoolean( "org.nlogo.noGenerator" ) )
			{
				throw new ExtensionException("The profiler extension requires the NetLogo Bytecode generator, which is currently turned off, see the org.nlogo.noGenerator property.");
			}
			
			Tracer.profilingTracer = tracer;
			tracer.enable();
		}
	}
	
	public static class ProfilerStop extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax() ;
		}
		public void perform(Argument args[], Context context) throws ExtensionException
		{
			tracer.disable();
		}
	
	}
	
	public static class ProfilerReset extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax() ;
		}
		public void perform(Argument args[], Context context) throws ExtensionException
		{
			tracer.reset();
		}
	
	}

	public static class ProfilerReport extends DefaultReporter
	{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(Syntax.TYPE_STRING) ;
		}
		public Object report(Argument args[], Context context) throws ExtensionException {
			java.io.ByteArrayOutputStream outArray = new java.io.ByteArrayOutputStream() ;
			java.io.PrintStream out = new java.io.PrintStream( outArray ) ;
			tracer.dump( out ) ;
			return outArray.toString() ;
		}
	}

	public static class ProfilerValues extends DefaultReporter
	{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(Syntax.TYPE_LIST) ;
		}
		public Object report(Argument args[], Context context) throws ExtensionException {
			return ((org.nlogo.extensions.profiler.QuickTracer)tracer).values();
		}
	}

	public static class ProfilerClass extends DefaultCommand
	{
		public Syntax getSyntax() {
			int[] right = {Syntax.TYPE_STRING};
			return Syntax.commandSyntax(right) ;
		}
		public void perform(Argument args[], Context context) throws ExtensionException, LogoException
		{
			loadTracer((String)args[0].get());
			Tracer.profilingTracer = tracer;
		}
	}
	
	private static void loadTracer(String classname) throws ExtensionException
	{
  		try
		{
			tracer = Tracer.initializeTracer( Class.forName( classname ) ) ;
		} catch( ClassNotFoundException ex ) {
			throw new ExtensionException("Cannot find tracer class " + classname);
		}
	}
}
