package eu.unicore.uas.security.gridmapfile;

/**
 * Convenience class for storing pairs of objects (e.g. Lists of mappings)
 *
 * @param <T1>
 * @param <T2>
 */
public class Pair<T1, T2> {

	private T1 m1;
	private T2 m2;

	public Pair() {
		super();
	}

	public Pair(T1 m1, T2 m2) {
		super();
		this.m1 = m1;
		this.m2 = m2;
	}

	public T1 getM1() {
		return m1;
	}

	public void setM1(T1 m1) {
		this.m1 = m1;
	}

	public T2 getM2() {
		return m2;
	}

	public void setM2(T2 m2) {
		this.m2 = m2;
	}

}

