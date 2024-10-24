// (c) 2007 Uri Wilensky. See README.txt for terms of use.

// This NetLogo extension provides an array data type.

package org.nlogo.extensions.array;

import org.nlogo.api.LogoException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.Argument;
import org.nlogo.api.Syntax;
import org.nlogo.api.Context;
import org.nlogo.api.LogoList;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.Reporter;

import java.util.Iterator;

public class ArrayExtension
	extends org.nlogo.api.DefaultClassManager
{
	private static class LogoArray
		extends java.util.ArrayList
		// new NetLogo data types defined by extensions must implement
		// this interface
		implements org.nlogo.api.ExtensionObject
	{
		LogoArray( java.util.Collection collection )
		{
			super( collection ) ;
		}
		public String dump(boolean readable, boolean exporting)
		{
			StringBuffer buf = new StringBuffer() ;
			boolean first = true ;
			for( Iterator it = iterator(); it.hasNext() ; )
			{
				if( ! first )
				{
					buf.append( " " ) ;
				}
				first = false ;
				buf.append
					( org.nlogo.agent.Dump.logoObject( it.next(), true, true ) ) ;
			}
			return buf.toString() ;
		}
		public String getExtensionName()
		{
			return "array";
		}
		public String getNLTypeName()
		{
			// since this extension only defines one type, we don't
			// need to give it a name; "array:" is enough,
			// "array:array" would be redundant
			return "";
		}
		public boolean recursivelyEqual( Object o )
		{
			if( ! ( o instanceof LogoArray) )
			{
				return false ;
			}
			LogoArray otherArray = (LogoArray) o ;
			if( size() != otherArray.size() )
			{
				return false ;
			}
			Iterator iter1 = iterator() ;
			Iterator iter2 = otherArray.iterator() ;
			while( iter1.hasNext() )
			{
				if( ! org.nlogo.agent.World.recursivelyEqual
					( iter1.next() , iter2.next() ) )
				{
					return false ;
				}
			}
			return true ;
		}
	}
	
	///

    public void load( org.nlogo.api.PrimitiveManager primManager )
    {
		int intArr[] = new int[] { Syntax.TYPE_WILDCARD, Syntax.TYPE_NUMBER };
		Reporter it= (new Item()).setSyntax(intArr, Syntax.TYPE_WILDCARD);
		// it.setSyntax(intArr, Syntax.TYPE_WILDCARD);
		primManager.addPrimitive("item", (new Item()).setSyntax(intArr, Syntax.TYPE_WILDCARD));
		primManager.addPrimitive( "set", new Set() );
		primManager.addPrimitive( "length", new Length() );
 		primManager.addPrimitive( "to-list", new ToList() );
 		primManager.addPrimitive( "from-list", new FromList() );
    }

	///
	

	public org.nlogo.api.ExtensionObject readExtensionObject( org.nlogo.api.ExtensionManager em ,
															  String typeName , String value )
		throws org.nlogo.api.ExtensionException
	{
		return new LogoArray
			( (LogoList) em.readFromString( "[" + value + "]" ) ) ;
	}

	private static abstract class PQR extends DefaultReporter
	{
			int[] argtypes;
			int rettype;

			public Reporter setSyntax(int[] a, int r)
			{
				argtypes = a;
				rettype = r;
				return(this);
			}
				
			public String getAgentClassString()
			{
				return "OTPL" ;
			}
			public Syntax getSyntax()
			{
				return Syntax.reporterSyntax
					( argtypes, rettype ) ;
			}
		}



	public static class Item extends PQR
	{
		// Item(int[] a, int b) { super(a, b); };

		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			Object arg0 = args[ 0 ].get() ;
			if( ! ( arg0 instanceof LogoArray ) )
			{
				throw new org.nlogo.api.ExtensionException
					( "not an array: " +
					  org.nlogo.agent.Dump.logoObject( arg0 ) ) ;
			}
			LogoArray array = (LogoArray) arg0 ;
			int index = args[ 1 ].getIntValue() ;
			if( index < 0 || index >= array.size() )
			{
				throw new org.nlogo.api.ExtensionException
					( index + " is not a valid index into an array of length "
					  + array.size() ) ;
			}
			return array.get( index ) ;
		}					      
    }

    public static class Set extends DefaultCommand
	{
		public Syntax getSyntax()
		{
			return Syntax.commandSyntax
				( new int[] { Syntax.TYPE_WILDCARD ,
							  Syntax.TYPE_NUMBER ,
							  Syntax.TYPE_WILDCARD } ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public void perform( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			Object arg0 = args[ 0 ].get() ;
			if( ! ( arg0 instanceof LogoArray ) )
			{
				throw new org.nlogo.api.ExtensionException
					( "not an array: " +
					  org.nlogo.agent.Dump.logoObject( arg0 ) ) ;
			}
			LogoArray array = (LogoArray) arg0 ;
			int index = args[ 1 ].getIntValue() ;
			if( index < 0 || index >= array.size() )
			{
				throw new org.nlogo.api.ExtensionException
					( index + " is not a valid index into an array of length "
					  + array.size() ) ;
			}
			array.set( index , args[ 2 ].get() ) ;
		}					      
    }

    public static class Length extends DefaultReporter
	{
		public Syntax getSyntax()
		{
			return Syntax.reporterSyntax
				( new int[] { Syntax.TYPE_WILDCARD } ,
				  Syntax.TYPE_NUMBER ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			Object arg0 = args[ 0 ].get() ;
			if( ! ( arg0 instanceof LogoArray ) )
			{
				throw new org.nlogo.api.ExtensionException
					( "not an array: " +
					  org.nlogo.agent.Dump.logoObject( arg0 ) ) ;
			}
			return new Double( ( (LogoArray) arg0 ).size() ) ;
		}					      
    }

	public static class ToList extends DefaultReporter
	{
		public Syntax getSyntax()
		{
			return Syntax.reporterSyntax
				( new int[] { Syntax.TYPE_WILDCARD } ,
				  Syntax.TYPE_LIST ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			Object arg0 = args[ 0 ].get() ;
			if( ! ( arg0 instanceof LogoArray ) )
			{
				throw new org.nlogo.api.ExtensionException
					( "not an array: " +
					  org.nlogo.agent.Dump.logoObject( arg0 ) ) ;
			}
			return new org.nlogo.api.LogoList( (LogoArray) arg0 ) ;
		}
	}

	public static class FromList extends DefaultReporter
	{
		public Syntax getSyntax()
		{
			return Syntax.reporterSyntax
				( new int[] { Syntax.TYPE_LIST } ,
				  Syntax.TYPE_WILDCARD ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			return new LogoArray( args[ 0 ].getList() ) ;
		}
	}

}
