package Showdown;

public class Challenge {

	public String name, gen;

	public Challenge(String name, String gen) {
		this.name = name;
		this.gen = gen;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Challenge) {
			Challenge c = (Challenge) o;
			return name.equals(c.name) && gen.equals(c.gen);
		}
		return false;
	}
}
