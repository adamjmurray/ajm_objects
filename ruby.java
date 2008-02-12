package ajm;

/*
 Copyright (c) 2008, Adam Murray (adam@compusition.com). All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, 
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

import java.io.File;

import org.apache.bsf.BSFException;

import ajm.util.RubyEvaluator;

import com.cycling74.max.Atom;
import com.cycling74.max.MaxObject;
import com.cycling74.max.MaxSystem;

/**
 * The ajm.ruby MaxObject
 * 
 * @version 0.7
 * @author Adam Murray (adam@compusition.com)
 */
public class ruby extends AbstractMaxObject {

	boolean verbose = false;
	int evaloutlet = 0;

	private RubyEvaluator ruby;
	private AbstractMaxObject thisObj = this;

	/**
	 * The Constructor
	 * 
	 * @param args
	 *            1 optional integer arg specifies the number of outlets
	 * 
	 * @throws BSFException
	 *             if a problem occurs evaluating the Ruby initialization code
	 */
	public ruby(Atom[] args) throws BSFException {
		int outlets = 1;
		if (args.length > 0 && args[0].isInt() && args[0].getInt() > 1) {
			outlets = args[0].getInt();
		}
		declareIO(1, outlets);
		createInfoOutlet(false);

		declareAttribute("verbose", "getverbose", "verbose");
		declareAttribute("evaloutlet", "getevaloutlet", "evaloutlet");

		String pathToJRuby = MaxSystem.locateFile("jruby.jar");
		String pathToCustomRubyScripts = null;
		if (pathToJRuby != null) {
			File jRubyDir = new File(pathToJRuby).getParentFile();
			// Set jruby.home to the Max installation's java directory, where it will look for lib/ruby
			System.setProperty("jruby.home", jRubyDir.getParent());
			pathToCustomRubyScripts = new File(jRubyDir, "ruby").toString();
		}

		ruby = new RubyEvaluator();
		if (getAttrBool("verbose")) {
			ruby.setVerboseOut(System.out);
		}
		ruby.declareBean("MaxObject", this, MaxObject.class);
		ruby.declareBean("Utils", new Utils(), Utils.class);

		CodeBuilder code = new CodeBuilder();

		if (pathToCustomRubyScripts != null) {
			code.line("$LOAD_PATH << '" + pathToCustomRubyScripts.replace("'", "\\'") + "'");
		}

		code.line("def puts str");
		code.line("  $Utils.puts str");
		code.line("end");

		code.line("def print str");
		code.line("  $Utils.print str");
		code.line("end");

		code.line("def flush");
		code.line("  $Utils.flush");
		code.line("end");

		code.line("def atom obj");
		code.line("  if obj==nil");
		code.line("    $Utils.emptyAtomArray");
		code.line("  else");
		code.line("    $Utils.atom obj");
		code.line("  end");
		code.line("end");

		code.line("def flush");
		code.line("  $Utils.flush");
		code.line("end");

		code.line("def error str");
		code.line("  $Utils.error str");
		code.line("end");

		code.line("def outlet n, *params");
		code.line("  $Utils.outlet n, params");
		code.line("end");

		ruby.eval(code.toString());
	}

	/*
	 * protected MaxQelem getInitializer() { return new MaxQelem(new Executable() { public void execute() { } }); }
	 */

	public void bang() {
		eval("bang");
	}

	public int getevaloutlet() {
		return evaloutlet;
	}

	public void evaloutlet(int evaloutlet) {
		if (evaloutlet >= getNumOutlets()) {
			err("Invalid evaloutlet " + evaloutlet);
		}
		else {
			this.evaloutlet = evaloutlet;
		}
	}

	public boolean getverbose() {
		return verbose;
	}

	public void verbose(boolean verbose) {
		this.verbose = verbose;
		ruby.setVerboseOut(verbose ? System.out : null);
	}

	public void list(Atom[] args) {
		eval(toString(args));
	}

	public void anything(String msg, Atom[] args) {
		eval(toString(msg, args));
	}

	private void eval(String input) {
		try {
			Atom[] value = ruby.evalToAtoms(input);
			if (evaloutlet >= 0) {
				outlet(evaloutlet, value);
			}
		}
		catch (BSFException e) {
			err("could not evaluate: " + input);
		}
	}

	private class CodeBuilder {
		private StringBuilder code = new StringBuilder();

		public void line(String s) {
			code.append(s).append("\n");
		}

		public String toString() {
			return code.toString();
		}
	}

	private class Utils {
		// JRuby has problems calling some Max Java methods, so I go back into Java-land to do it

		public Object atom(Object o) {
			Atom[] atoms = ruby.toAtoms(o);
			if (atoms.length == 1) {
				return atoms[0];
			}
			else {
				return atoms;
			}
		}

		public Atom[] emptyAtomArray() {
			return Atom.emptyArray;
		}

		public void puts(Object o) {
			System.out.println(o.toString());
		}

		public void print(Object o) {
			System.out.print(o.toString());
		}

		public void error(Object o) {
			thisObj.err(o.toString());
		}

		public void flush() {
			System.out.println();
		}

		public void outlet(int outletIdx, Object output) {
			if (outletIdx >= thisObj.getNumOutlets()) {
				err("Invalid outlet index " + outletIdx);
			}
			else {
				thisObj.outlet(outletIdx, ruby.toAtoms(output));
			}
		}
	}
}