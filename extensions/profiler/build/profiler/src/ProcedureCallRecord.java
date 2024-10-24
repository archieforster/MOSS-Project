/** (c) 2007 Uri Wilensky. See README.txt for terms of use. **/

package org.nlogo.extensions.profiler ;

public class ProcedureCallRecord extends CallRecord
{
	public String fileName ;
	public int pos ;

	public ProcedureCallRecord( org.nlogo.command.Procedure proc, String agent, String[] argDescriptions )
	{
		super(proc.name, agent, argDescriptions);
		this.pos = proc.pos ;
		this.argDescriptions = argDescriptions ;
	}
}
