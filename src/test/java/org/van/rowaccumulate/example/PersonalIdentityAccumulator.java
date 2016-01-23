package org.van.rowaccumulate.example;

import org.van.rowaccumulate.Accumulator;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An accumulator to emit Identity entities, to be chained to a parent Accumulator of type Person so that
 * the instance can append its emitted entities into the Person being accumulated.
 * <br>
 * An alternative to explicitly defining a class, we can also use anonymous classes (see the test class on
 * how it handles dishes and contrast with how we handle Identities in this class).
 *
 * Created by vly on 9/4/2015.
 */
public class PersonalIdentityAccumulator<R,K> extends Accumulator<Person,Identity,R,K> {

    public PersonalIdentityAccumulator(Function<R, K> rowKeyExtractor,
        Function<Identity, K> accumulatedRowExtractor,
        Function<R, Identity> accumulateMapper,
        Consumer<Identity> emitter) {
        super(rowKeyExtractor, accumulatedRowExtractor, accumulateMapper, emitter);
    }

    @Override
    protected Optional<Identity> transition(Optional<Person> person, Optional<R> row) {
        Optional<Identity> identity = super.transition(person, row);
        if (person.isPresent() && identity.isPresent()) {
            person.get().identityList.add(identity.get());
        }
        return identity;
    }
}
