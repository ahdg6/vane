package org.oddlama.vane.core.functional;

import java.util.List;

@FunctionalInterface
public interface Consumer2<T1, T2> extends ErasedFunctor, GenericsFinder {
	void apply(T1 t1, T2 t2);

	@Override
	@SuppressWarnings("unchecked")
	default public Object invoke(List<Object> args) {
		if (args.size() != 2) {
			throw new IllegalArgumentException("Functor needs 2 arguments but got " + args.size() + " arguments");
		}
		apply((T1)args.get(0), (T2)args.get(1));
		return null;
	}
}
