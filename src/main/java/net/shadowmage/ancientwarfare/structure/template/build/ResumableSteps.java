package net.shadowmage.ancientwarfare.structure.template.build;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 * Improvement (not baseline, budgeted_placement): work that the original code does in one loop (terrain preparation of
 * a town, a validator's pre-/postGeneration) cut into units that run in exactly the original order, resumable at any
 * unit boundary. A step is a sequence of phases; a phase is an iterator of units created lazily when the phase is
 * reached (so anything it computes up front is computed at the same point of the sequence as in the original code).
 * run(deadline, ...) works units until the deadline (System.nanoTime) has passed, checked every checkEvery units of
 * the current phase, and always works at least one unit per call.
 */
public final class ResumableSteps {
	private final Deque<Phase> phases = new ArrayDeque<>();
	private Phase current;
	private long units;

	private record Phase(Supplier<Iterator<Runnable>> supplier, int checkEvery, Iterator<Runnable>[] holder) {
	}

	public static ResumableSteps of(Runnable unit) {
		return new ResumableSteps().then(unit);
	}

	/* one unit */
	public ResumableSteps then(Runnable unit) {
		return thenUnits(() -> Collections.singletonList(unit).iterator(), 1);
	}

	/* a phase of units; checkEvery: deadline check every n units (1 for heavy units such as a whole column) */
	@SuppressWarnings("unchecked")
	public ResumableSteps thenUnits(Supplier<Iterator<Runnable>> units, int checkEvery) {
		phases.add(new Phase(units, Math.max(1, checkEvery), new Iterator[1]));
		return this;
	}

	/*
	 * a phase that ends the current call until ready returns true (asked once per call); with unlimited (server stop)
	 * it is skipped, so the next phase must cope with whatever ready was waiting for not being there
	 */
	public ResumableSteps thenWait(java.util.function.BooleanSupplier ready) {
		return thenUnits(() -> new WaitIterator(ready), 1);
	}

	/* a phase that is itself a ResumableSteps, created when reached */
	public ResumableSteps thenSteps(Supplier<ResumableSteps> steps) {
		return thenUnits(() -> new NestedIterator(steps.get()), 1);
	}

	/* a phase of units mapped from positions (or any element) in iteration order */
	public <T> ResumableSteps thenEach(Supplier<Iterable<T>> elements, Function<T, Runnable> unit, int checkEvery) {
		return thenUnits(() -> {
			Iterator<T> it = elements.get().iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Runnable next() {
					return unit.apply(it.next());
				}
			};
		}, checkEvery);
	}

	/* a phase of prepared units (built in the loop order of the original method) */
	public ResumableSteps thenList(Supplier<List<Runnable>> units, int checkEvery) {
		return thenUnits(() -> units.get().iterator(), checkEvery);
	}

	public long getUnits() {
		return units;
	}

	public boolean isDone() {
		return current == null && phases.isEmpty();
	}

	/*
	 * returns true once every unit has run; unlimited = run to the end now
	 */
	public boolean run(long deadlineNanos, boolean unlimited) {
		int count = 0;
		while (true) {
			if (current == null) {
				current = phases.poll();
				if (current == null) {
					return true;
				}
				current.holder()[0] = current.supplier().get();
			}
			Iterator<Runnable> it = current.holder()[0];
			if (!it.hasNext()) {
				current = null;
				continue;
			}
			if (!unlimited && count > 0 && count % current.checkEvery() == 0 && System.nanoTime() - deadlineNanos >= 0) {
				return false;
			}
			if (it instanceof WaitIterator wait) {
				if (!unlimited && !wait.ready.getAsBoolean()) {
					return false;
				}
				wait.done = true;
				continue;
			}
			if (it instanceof NestedIterator nested) {
				//a nested step is worked with the same deadline; it decides its own unit boundaries
				long before = nested.steps.units;
				boolean nestedDone = nested.steps.run(deadlineNanos, unlimited);
				count += (int) (nested.steps.units - before);
				units += nested.steps.units - before;
				if (!nestedDone) {
					return false;
				}
				continue;
			}
			it.next().run();
			count++;
			units++;
		}
	}

	private static final class WaitIterator implements Iterator<Runnable> {
		private final java.util.function.BooleanSupplier ready;
		private boolean done;

		private WaitIterator(java.util.function.BooleanSupplier ready) {
			this.ready = ready;
		}

		@Override
		public boolean hasNext() {
			return !done;
		}

		@Override
		public Runnable next() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class NestedIterator implements Iterator<Runnable> {
		private final ResumableSteps steps;

		private NestedIterator(ResumableSteps steps) {
			this.steps = steps;
		}

		@Override
		public boolean hasNext() {
			return !steps.isDone();
		}

		@Override
		public Runnable next() {
			throw new UnsupportedOperationException();
		}
	}
}
