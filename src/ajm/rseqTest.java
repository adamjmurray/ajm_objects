package ajm;

import ajm.seqsupport.Item;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.*;

import com.cycling74.max.Atom;
import com.cycling74.max.Executable;

public class rseqTest {

	private boolean DEBUG = false;
	private static PrintStream out;

	@BeforeClass
	public static void initOut() {
		out = new PrintStream(System.out);
	}

	protected class rStub extends rseq {

		public rStub(Atom[] args) {

			super(args);
			if (DEBUG) {
				setDebugOut(out);
			}
		}

		// Sending to outlet will not work inside jUnit tests:
		@Override
		protected void output(OUTLET outlet, Atom data) {
			if (outlet == OUTLET.VALUE) {
				valOuts.add(data.getInt());
				if (DEBUG) {
					out.println(outlet + ": " + data.getInt());
				}
			}
			outputs.put(outlet, data.getInt());
		}

		@Override
		protected void output(OUTLET outlet, Item data) {
			Object val = data.getValue();
			if (val instanceof Atom) {
				Atom atom = (Atom) val;
				if (outlet == OUTLET.VALUE) {
					valOuts.add(atom.getInt());
					if (DEBUG) {
						out.println(outlet + ": " + atom.getInt());
					}
				}
				outputs.put(outlet, atom.getInt());
			}
			// else?
		}

		@Override
		protected void output(OUTLET outlet, Atom[] data) {
		}

		@Override
		protected void output(OUTLET outlet, int data) {
			outputs.put(outlet, data);
			if (DEBUG) {
				out.println(outlet + ": " + data);
			}
		}

		@Override
		protected void outputInfo(Atom... data) {
		}

		@Override
		protected Executable getInitializer() {
			return null;
		}

		private Map<OUTLET, Integer> outputs = new HashMap<OUTLET, Integer>();
		private List<Integer> valOuts = new ArrayList<Integer>();

		public int[] getValueOutputs() {
			int[] vo = new int[valOuts.size()];
			for (int i = 0; i < valOuts.size(); i++) {
				vo[i] = valOuts.get(i);
			}
			return vo;
		}

		public Integer valueSinceBang() {
			return outputs.get(OUTLET.VALUE);
		}

		public Integer indexSinceBang() {
			return outputs.get(OUTLET.INDEX);
		}

		public Integer iterationSinceBang() {
			return outputs.get(OUTLET.ITER);
		}

		@Override
		public void bang() {
			outputs.clear();
			super.bang();
		}

		protected void set(int... vals) {
			set(Atom.newAtom(vals));
		}
	}

	protected rStub makeSeq(int... vals) {
		rStub s = new rStub(Atom.emptyArray);
		s.set(Atom.newAtom(vals));
		return s;
	}

	protected rStub emptySeq() {
		return makeSeq(new int[] {});
	}

	protected void assertEquals(int i1, Integer i2) {
		if (i2 == null) {
			Assert.assertEquals(i1, null);
		}
		else {
			Assert.assertEquals(i1, i2.intValue());
		}
	}

	protected void assertEquals(int[] actualIntArray, int... expectedInts) {
		Assert.assertTrue(Arrays.equals(expectedInts, actualIntArray));
	}

	@Test
	public void testSequence() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testSequence ***");
		}
		rStub s = makeSeq(1, 2, 3);
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(1, s.valueSinceBang());
		s.bang();
		assertEquals(2, s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(3, s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), 1, 2, 3, 1);
		if (DEBUG) {
			out.println("*** END testSequence ***\n");
		}
	}

	@Test
	public void testShortenList() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testShortenList ***");
		}
		rStub s = makeSeq(1, 1);
		s.bang();
		assertEquals(0, s.iter);

		s.seq(Atom.newAtom(new int[] { 1 }));
		s.bang();
		assertEquals(1, s.iter);
		s.bang();
		assertEquals(2, s.iter);

		if (DEBUG) {
			out.println("*** END testShortenList ***\n");
		}
	}

	@Test
	public void testSetIndex() {
		if (DEBUG) {
			out.println("\n*** START testSetIndex ***");
		}

		rStub s = makeSeq(1, 2, 3);
		assertNull(s.valueSinceBang());

		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(0, s.indexSinceBang());
		assertEquals(s.getValueOutputs().length, 1);

		s.bang();
		assertEquals(2, s.valueSinceBang());
		assertEquals(1, s.indexSinceBang());
		assertEquals(s.getValueOutputs(), 1, 2);

		s.index(0);
		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(0, s.indexSinceBang());
		assertEquals(s.getValueOutputs(), 1, 2, 1);

		if (DEBUG) {
			out.println("*** END testSetIndex ***\n");
		}
	}

	@Test
	public void testSetIndexWithZeroValue() {

		if (DEBUG) {
			out.println("\n*** START testSetWithZero ***");
		}

		rStub s = makeSeq(1, 1, 0, 1);
		assertNull(s.valueSinceBang());

		s.bang();
		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(1, s.indexSinceBang());
		assertEquals(s.getValueOutputs(), 1, 1);

		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(3, s.indexSinceBang());
		assertEquals(s.getValueOutputs(), 1, 1, 0, 1);

		s.index(2);
		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(3, s.indexSinceBang());

		assertEquals(s.getValueOutputs(), 1, 1, 0, 1, 0, 1);

		if (DEBUG) {
			out.println("*** END testSetIndexWithZero ***\n");
		}
	}

	@Test
	public void testSetIndexToZero() {
		if (DEBUG) {
			out.println("\n*** START testSetIndexToZero ***");
		}
		rStub s = makeSeq(1, 2, 3);
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(1, s.valueSinceBang());
		s.bang();
		assertEquals(2, s.valueSinceBang());
		s.index(0);
		s.bang();
		assertEquals(1, s.valueSinceBang());
		s.bang();
		assertEquals(2, s.valueSinceBang());

		assertEquals(s.getValueOutputs(), 1, 2, 1, 2);
		if (DEBUG) {
			out.println("*** END testSetIndexToZero ***\n");
		}
	}

	@Test
	public void testNegativeValue() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testNegativeValue ***");
		}
		rStub s = makeSeq(1, -2, 3);
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(1, s.valueSinceBang());
		s.bang();
		assertEquals(-2, s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(3, s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertNull(s.valueSinceBang());
		s.bang();
		assertEquals(1, s.valueSinceBang());

		assertEquals(s.getValueOutputs(), 1, -2, 3, 1);
		if (DEBUG) {
			out.println("*** END testNegativeValue ***\n");
		}
	}

	@Test
	public void testZeroValue() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testZeroValue ***");
		}

		rStub s = makeSeq(1, 0);
		assertNull(s.valueSinceBang());

		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), new int[] { 1 });

		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), 1, 0, 1);

		s.bang();
		assertEquals(1, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), 1, 0, 1, 0, 1);

		if (DEBUG) {
			out.println("*** END testZeroValue ***\n");
		}
	}

	@Test
	public void testInterleavedZero() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testInterleavedZero ***");
		}
		rStub s = makeSeq(2, 0, 2, 0);

		// out.println("***");
		s.bang();
		assertEquals(2, s.valueSinceBang());
		// out.println("***");

		s.bang();
		assertNull(s.valueSinceBang());
		// out.println("***");

		s.bang();
		assertEquals(2, s.valueSinceBang());
		// out.println("***");

		s.bang();
		assertNull(s.valueSinceBang());

		s.bang();
		assertEquals(2, s.valueSinceBang());

		assertEquals(s.getValueOutputs(), 2, 0, 2, 0, 2);
		if (DEBUG) {
			out.println("*** END testInterleavedZero ***\n");
		}
	}

	@Test
	public void testPreventInfiniteLoop() throws Exception {
		if (DEBUG) {
			out.println("\n*** START testPreventInfiniteLoop ***");
		}

		rStub s = makeSeq(0, 0, 0);
		Assert.assertEquals(makeSeq(0, 0, 0), s);
		assertNull(s.valueSinceBang());

		s.bang();
		assertEquals(0, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), 0, 0, 0);

		s.bang();
		assertEquals(0, s.valueSinceBang());
		assertEquals(s.getValueOutputs(), 0, 0, 0, 0, 0, 0);

		if (DEBUG) {
			out.println("*** END testPreventInfiniteLoop ***\n");
		}
	}

	// TODO: test arpeggiated chords

}
