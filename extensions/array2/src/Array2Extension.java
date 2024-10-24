// (c) 2007 Uri Wilensky. See README.txt for terms of use.

// This NetLogo extension provides an array data type;
// new, set-items and auto-array-extension added by roger.peppe@ncl.ac.uk, 2008
import org.nlogo.api.LogoException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.Argument;
import org.nlogo.api.Syntax;
import org.nlogo.api.Context;
import org.nlogo.api.LogoList;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.DefaultCommand;
import java.util.Iterator;

public class Array2Extension
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
		
		LogoArray( int size, Object o )
		{
			super( size ) ;
			for( int i = 0; i < size; i++ )
			{
				this.add(o);
			}
		}
		
		public String dump(boolean readable, boolean exporting)
		{
			StringBuffer buf = new StringBuffer() ;
			Iterator it = this.iterator();
			boolean hasnext = it.hasNext();
			while( hasnext )
			{
				buf.append
					( org.nlogo.agent.Dump.logoObject( it.next(), true, true ) ) ;
				hasnext = it.hasNext();
				if( hasnext )
					buf.append( " " ) ;
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
	
	public void load( org.nlogo.api.PrimitiveManager primManager )
	{
		primManager.addPrimitive( "item", new Item() );
		primManager.addPrimitive( "new", new NewArray() );
		primManager.addPrimitive( "set", new Set() );
		primManager.addPrimitive( "length", new Length() );
 		primManager.addPrimitive( "to-list", new ToList() );
 		primManager.addPrimitive( "from-list", new FromList() );
 		primManager.addPrimitive( "set-items", new SetItems() );
	}

	public org.nlogo.api.ExtensionObject readExtensionObject(
			org.nlogo.api.ExtensionManager em,
			String typeName,
			String value )
		throws org.nlogo.api.ExtensionException
	{
		return new LogoArray
			( (LogoList) em.readFromString( "[" + value + "]" ) ) ;
	}
	
	public static LogoArray getArrayValue(Argument arg) throws org.nlogo.api.ExtensionException, org.nlogo.api.LogoException
	{
		Object obj = arg.get();
		if( ! ( obj instanceof LogoArray ) )
		{
			throw new org.nlogo.api.ExtensionException
				( "not an array: " +
				  org.nlogo.agent.Dump.logoObject( obj ) ) ;
		}
		return (LogoArray)obj;
	}

	public static class Item extends DefaultReporter
	{
		public Syntax getSyntax()
		{
			return Syntax.reporterSyntax
				( new int[] { Syntax.TYPE_WILDCARD ,
							  Syntax.TYPE_NUMBER } ,
				  Syntax.TYPE_WILDCARD ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			LogoArray array = getArrayValue( args[ 0 ] );
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
			LogoArray array = getArrayValue( args[ 0 ] );
			int index = args[ 1 ].getIntValue() ;
			int size = array.size();
			if( index < 0 || index >= size )
			{
				if( index > size )
					throw new org.nlogo.api.ExtensionException
						( index + " is not a valid index into an array of length "
						  + array.size() ) ;
				array.add(args[ 2 ].get() );
			}else
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
			return new Double( ( getArrayValue(args[ 0 ]) ).size() ) ;
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
			return new org.nlogo.api.LogoList( getArrayValue( args[ 0 ] )) ;
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

    public static class SetItems extends DefaultCommand
	{
		public Syntax getSyntax()
		{
			return Syntax.commandSyntax
				( new int[] { Syntax.TYPE_WILDCARD ,
							  Syntax.TYPE_NUMBER ,
							  Syntax.TYPE_WILDCARD,
							  Syntax.TYPE_NUMBER,
							  Syntax.TYPE_NUMBER | Syntax.TYPE_OPTIONAL } ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public void perform( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			LogoArray a0 = getArrayValue( args[ 0 ] );
			int a0start = args[ 1 ].getIntValue() ;
			LogoArray a1 = getArrayValue( args [ 2 ] );
			int a1start = args[ 3 ].getIntValue();
			int a1end;
			if( args.length > 4 )
				a1end = args[ 4 ].getIntValue();
			else
				a1end = a1.size();
			int n = a1end - a1start;
			int size0 = a0.size();
			int size1 = a1.size();
			
			if( n < 0 || a1start < 0 || a1start > size1 || a1end > size1 ||
				a0start < 0 || a0start > size0 )
			{
				throw new org.nlogo.api.ExtensionException( "index out of bounds" ) ;
			}
			a0.ensureCapacity( a0start + n );
			int add = a0start + n - size0;
			if( add > 0 )
				n = size0 - a0start;
			int i0 = a0start;
			int i1 = a1start;
			for(; n > 0; n--)
				a0.set( i0++, a1.get( i1++ ) );
			for(; add > 0; add--)
				a0.add( a1.get( i1++ ) );
		}			      
	}

	public static class NewArray extends DefaultReporter
	{
		public Syntax getSyntax()
		{
			return Syntax.reporterSyntax
				( new int[] { Syntax.TYPE_NUMBER, Syntax.TYPE_WILDCARD } ,
				  Syntax.TYPE_WILDCARD ) ;
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}
		public Object report( Argument args[] , Context context )
			throws ExtensionException , LogoException
		{
			return new LogoArray( args[ 0 ].getIntValue(), args[ 1 ].get() ) ;
		}
	}
}
