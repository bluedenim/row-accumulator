package org.van.rowaccumulate;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Given a list of records, this class is used to iterate each record (via a call to
 * {@link #accumulate(Object)}) and aggregate them into instances of T. A new instance of T is
 * created when a new K is detected in the records accumulated.
 * <br>
 * Accumulators can be chained to build up sub-collections.
 * <br>
 * NOTE that rows MUST BE ordered/grouped by keys as calculated by the key extraction closure since
 * a change in the key is interpreted as the end of the current T and the beginning of a
 * new instance.
 * <br>
 * Example of a set of records for (ID, firstName, lastName, dish):
 * <pre>
 * {@code
 *     1, Slim, Joe, Chicken
 *     1, Slim, Joe, Chicken
 *     2, Portly, Bob, Steak
 *     2, Portly, Bob, Fish
 *     2, Portly, Bob, Spinach
 * }
 * </pre>
 * An accumulator can be used to model a Person entity based on the first 3 fields, then an
 * accumulator can be chained to model a Dish entity which appends to the dishes collection
 * of the Person entity. The result can be:
 * <ul>
 *     <li>Person: {1, Joe Slim} with a collection of Dish: {Chicken}</li>
 *     <li>Person: {2, Bob Portly} with a collection of Dish: {Steak, Fish, Spinach}</li>
 * </ul>
 *
 * @param <P> the type of the parent entity (for chaining)
 * @param <T> the type of the entity to build off of the rows
 * @param <R> the type of the rows
 * @param <K> the type of the key used to partition the rows corresponding to an entity
 */
public class Accumulator<P,T,R,K> {

    Function<R,K> rowKeyExtractor;
    Function<T,K> accumulatedKeyExtractor;
    Function<R,T> accumulateMapper;
    Consumer<T> emitter;

    // accumulation entity
    T accumulated;

    List<Accumulator> chained;

    /**
     * Create an instance of the accumulator with the various operational closures
     *
     * @param rowKeyExtractor closure to compute a key from a row
     * @param accumulatedRowExtractor closure to access the current key from an instance of T
     * @param accumulateMapper maps a row into an instance of T
     * @param emitter closure to emit an instance of T created from a row of data. NOTE that, if there
     *                are contiguous rows of data corresponding to the same instance (i.e. matching "key" properties),
     *                then this instance will not be complete yet because those subsequent rows have yet to be
     *                processed. Only when processing a new entity (or end of data is reached) will the current entity
     *                be completely built.
     */
    public Accumulator(
        Function<R,K> rowKeyExtractor, Function<T,K> accumulatedRowExtractor,
        Function<R,T> accumulateMapper,
        Consumer<T> emitter) {
        accumulated = null;

        this.rowKeyExtractor = rowKeyExtractor;
        this.accumulatedKeyExtractor = accumulatedRowExtractor;
        this.accumulateMapper = accumulateMapper;
        this.emitter = emitter;
        this.chained = new LinkedList<>();
    }

    /**
     * Create an instance of the accumulator with the various operational closures. Since this variant doesn't
     * accept an emitter, the only way the caller will be able to access any accumulated value is to subclass
     * and override {@link #transition(Optional, Object)}, so this ctor will be protected.
     *
     * @param rowKeyExtractor closure to compute a key from a row
     * @param accumulatedRowExtractor closure to access the current key from an instance of T
     * @param accumulateMapper maps a row into an instance of T
     */
    protected Accumulator(
        Function<R,K> rowKeyExtractor, Function<T,K> accumulatedRowExtractor,
        Function<R,T> accumulateMapper) {
        this(rowKeyExtractor, accumulatedRowExtractor, accumulateMapper, null);
    }

    /**
     * Sets the emitter closure for emitting any new accumulated values. NOTE that the new accumulated value/entity
     * MAY NOT be fully completed yet because subsequent rows have yet to be processed. Only when processing a
     * new entity (or end of data is reached) will the current entity be completely built.
     *
     * @param emitter a closure to accept a new accumulated value/entity
     *
     * @return this instance
     */
    public Accumulator<P,T,R,K> withEmitter(Consumer<T> emitter) {
        this.emitter = emitter;
        return this;
    }

    /**
     * Chain the provided accumulator to this accumulator. As this accumulator works with rows
     * of data, it calls the chained accumulators at the appropriate times to allow the chained
     * accumulators to handle subsets of data.
     *
     * @param a the accumulator to chain into this one
     *
     * @return this instance
     */
    public Accumulator withChained(Accumulator a) {
        chained.add(a);
        return this;
    }

    /**
     * Accumulate a row of data
     *
     * @param row the row of data to accumulate (if null, it will be ignored)
     */
    public void accumulate(R row) {
        accumulate(Optional.<P>empty(), Optional.ofNullable(row));
    }

    /**
     * Accumulate a row of data
     *
     * @param parentAccumulated the accumulated entity from the parent accumulator, if any
     *                          (used for chaining)
     * @param row the row of data or empty if this is the end of data.
     */
    protected void accumulate(Optional<P> parentAccumulated, Optional<R> row) {
        if (row.isPresent()) {
            if ( (null == accumulated) ||
                 !Objects.equals(rowKeyExtractor.apply(row.get()), accumulatedKeyExtractor.apply(accumulated)) ) {
                // A new value should now be accumulated. A new (or first) accumulated value should be
                // created for this row
                transition(parentAccumulated, row.get());
            } else {
                for (Accumulator a: chained) {
                    a.accumulate(Optional.of(accumulated), row);
                }
            }
        }
    }

    /**
     * Emit whatever accumulated entity so far and start a new entity based on the row of
     * data provided. Chaining accumulators typically will aggregate their accumulated entity
     * into the parent accumulated entity.
     *
     * @param parentAccumulated when chaining, this is the parent's accumulated value
     *                          if available.
     * @param row the data to create a new entity for, can be null when the row signifies
     *            the end of the data stream.
     *
     * @return the emitted instance of T if available
     */
    protected Optional<T> transition(Optional<P> parentAccumulated, R row) {
        accumulated = accumulateMapper.apply(row);
        Optional<T> toBeEmitted = Optional.ofNullable(accumulated);
        if (toBeEmitted.isPresent()) {
            for (Accumulator c: chained) {
                c.transition(toBeEmitted, row);
            }
            if (null != emitter){
                emitter.accept(toBeEmitted.get());
            }
        }
        return toBeEmitted;
    }

}
