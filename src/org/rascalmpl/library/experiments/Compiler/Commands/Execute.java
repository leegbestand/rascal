package org.rascalmpl.library.experiments.Compiler.Commands;

import java.io.StringWriter;
import java.net.URISyntaxException;

import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.ExecutionTools;
import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.RVMExecutable;
import org.rascalmpl.library.experiments.Compiler.RVM.Interpreter.RascalExecutionContext;
import org.rascalmpl.value.ISourceLocation;
import org.rascalmpl.value.IValue;

public class Execute extends Command {
	
	static ISourceLocation findBinary(String moduleName){
		ISourceLocation binDir = (ISourceLocation) commandKwArgs.get("binDir");
		StringWriter sw = new StringWriter();
		sw.append(binDir.getPath())
		  .append("/")
		  .append(moduleName.replaceAll("::", "/"))
		  .append(".rvm.ser.gz");
		try {
			return vf.sourceLocation(binDir.getScheme(), binDir.getAuthority(), sw.toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	
	/**
	 * Main function for execute command: rascal
	 * 
	 * @param args	list of command-line arguments
	 */
	public static void main(String[] args) {
		
		handleArgs("rascal", args);
		
		RascalExecutionContext rex = new RascalExecutionContext(vf, System.out, System.err);
		rex.setCurrentModuleName("Execute");
		RVMExecutable mainExecutable = RVMExecutable.read(findBinary(mainModule.getValue()));
	
		IValue res = ExecutionTools.executeProgram(mainExecutable, null, rex);
	}
}
