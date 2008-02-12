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

import java.util.Arrays;
import java.util.List;

import ajm.data.Item;
import ajm.data.MappedList;

import com.cycling74.max.Atom;
import com.cycling74.max.MaxQelem;

/**
 * The ajm.rseq MaxObject
 * 
 * @version 0.7
 * @author Adam Murray (adam@compusition.com)
 */
public class rseq extends seq {

	public rseq(Atom[] args) {
		super(); // declares seq attributes
		declareAttribute("valindex", "getvalindex", "valindex");

		declareIO(1, 5);
		setInletAssist(new String[] { "list / bang / commands" });
		setOutletAssist(new String[] { "value", "rhythmic index", "iter", "sequence", "value index" });

		parser.setEvalNotes(false);
	}

	@Override
	protected MaxQelem getInitializer() {
		return new MaxQelem(new rseqInitializationCallback());
	}

	protected class rseqInitializationCallback extends seqInitializationCallback {
		public void execute() {
			defaultRIndex = rIndex;
			/*
			 * if (!tickMap.isEmpty()) { defaultTickMap = tickMap.clone(); }
			 */
			super.execute();
		}
	}

	/*------------------------------------------------
	 *  Internal State
	 *------------------------------------------------*/

	// Attributes
	// rIndex is the rhythmic index, which is the "index" to Max.
	// The index in the superclass (seq) is the "valindex" for this class.
	protected int rIndex = 0;

	// Defaults (for reset commands)
	protected int defaultRIndex = rIndex;
	// protected MappedList<Integer> defaultTickMap = new MappedList<Integer>(); // ??? no default seq?

	// other internal state
	protected int maxRIndex;
	boolean initialOutput = true;
	protected int initialIdx = 0;
	protected MappedList<Integer> tickMap = new MappedList<Integer>();

	/*------------------------------------------------
	 *  Attribute Handlers
	 *------------------------------------------------*/

	@Override
	public int getindex() {
		return rIndex;
	}

	@Override
	public void index(int index) {
		this.rIndex = index;
	}

	public int getvalindex() {
		return super.getindex();
	}

	public void valindex(int index) {
		super.index(index);
	}

	@Override
	protected void onSeqChange() {
		super.onSeqChange();
		if (seq.size() > 0) {
			boolean allZeros = true;
			for (Item item : seq) {
				if (!item.isAtomArray()) {
					Atom a = item.getAtom();
					if ((a.isInt() || a.isFloat()) && a.toInt() != 0) {
						allZeros = false;
						break;
					}
				}
			}
			if (allZeros) {
				seq.add(new Item(1)); // prevents infinite loops
			}
		}

		tickMap.clear();
		maxRIndex = getIdxCount();
		int pos = 0;
		for (int i = 0; i < seq.size(); i++) {
			Item item = seq.get(i);
			if (pos == maxRIndex) {
				// then we exceeded the number of indexes, and these occur on index 0, but
				// not during the initial playback because they end the list rather than start it
				// the special -1 index is used to distinguish trailing 0s (or messages) from leading 0s (or messages)
				tickMap.addValue(-1, i);
			}
			else {
				tickMap.addValue(pos, i);
			}

			Atom a = item.getAtom();
			if (a.isInt() || a.isFloat()) {
				int delay = a.toInt();
				delay = (delay < 0) ? -delay : delay;
				pos += delay;
			}
		}
		initialOutput = true;
	}

	private int getIdxCount() {
		int total = 0;
		for (Item i : seq) {
			if (!i.isAtomArray()) {
				Atom a = i.getAtom();
				if (a.isInt() || a.isFloat()) {
					int val = a.toInt();
					total += (val < 0) ? -val : val;
				}
			}
		}
		return total;
	}

	/*
	 * Sets the length of the sequence in terms of the total number of ticks represented by the rhythm. If the new
	 * length is longer then the current, the last value will be increased as needed. Otherwise, seq will be removed
	 * from the end of the sequence, and the last remaining value decreased as needed in order to set the new rhythm
	 * length.
	 */
	// TODO: update patches
	public void rlength(int length) {
		this.maxRIndex = length;
	}

	/*
	 * try just setting a tick offset? public void rrotate(int ticks) { outputVals(); }
	 */

	@Override
	public void bang() {
		if (maxRIndex > 0) {
			output();
			rIndex += step;
		}
	}

	@Override
	public void output() {
		if (maxRIndex > 0) {
			fixIdxBounds();
			if (iterChanged) {
				iterChanged = false;
				output(OUTLET.ITER, iter);
			}
			output(OUTLET.INDEX, rIndex);
			if (initialOutput) {
				initialOutput = false;
			}
			else if (rIndex == 0) {
				outputValuesAtTick(-1);
			}
			outputValuesAtTick(rIndex);
		}
	}

	private void fixIdxBounds() {
		while (rIndex >= maxRIndex) {
			iterChanged = true;
			iter++;
			rIndex -= maxRIndex;
		}
		while (rIndex < 0) {
			iterChanged = true;
			iter--;
			rIndex += maxRIndex;
		}
	}

	private void outputValuesAtTick(int tick) {
		List<Integer> indexes = tickMap.get(tick);
		if (indexes != null) {
			for (Integer index : indexes) {
				output(OUTLET.VALINDEX, index);
				output(OUTLET.VALUE, seq.get(index).getAtom());
			}
		}
	}

	@Override
	public void reset() {
		resetvalindex();
		super.reset();
	}

	@Override
	public void resetindex() {
		rIndex = defaultRIndex;
		initialOutput = true; // This needs to be here for syncing to work right
	}

	public void resetvalindex() {
		super.resetindex();
	}

	public boolean equals(Object obj) {
		if (obj instanceof rseq) {
			return Arrays.equals(getseq(), ((rseq) obj).getseq());
		}
		else {
			return false;
		}
	}
}