package org.van.rowaccumulate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.van.rowaccumulate.example.*;

import java.util.*;

/**
 * Unit test doubling as a demo of how to use the Accumulator class.
 * <br>
 * See the README.md first for the background, then look at how this class sets up the Accumulators to handle Person
 * entities with aggregated Dish and Identity entities.
 *
 * Created by vly on 9/5/2015.
 */
public class AccumulatorTest {

    private List<Person> people = new LinkedList<>();
    private List<Person> peopleUsingPrevAcc = new LinkedList<>();

    @Before
    public void setUp() {

    }

    @Test
    public void testMappedData() {
        Accumulator<Void,Person,Map<String, String>,Long> personAcc = new Accumulator<>(
            (Map<String, String> map) -> new Long(map.get("id")),  // rows should have an "id" property
            Person::getId,                                         // mapped to Person's id attribute
            Person::fromMap,                                       // Person has a helper we can use
            people::add                                            // add to the list of people above
        );
        // Test the optional prev emitter as well. The two lists built using the normal emitter and the prev
        // emitter should yield the same value.
        personAcc.withPrevEmitter(peopleUsingPrevAcc::add);

        // This example uses an explicit derivative of the Accumulator class in cases where we may want to maintain
        // additional states in an Accumulator instance.
        Accumulator identityAcc = new PersonalIdentityAccumulator<>(
            (Map<String, String> map) -> map.get("identity_type"), // rows should have an "identity_type" property
            Identity::getType,                                     // mapped to an Identity's "type" attribute
            (Map<String, String> map) -> {                         // literal closure instead of a helper somewhere else
                Identity identity = new Identity();
                identity.type = map.get("identity_type");
                identity.value = map.get("identity_value");
                return identity;
            },
            i -> {}                                                // No-op emitter
        );

        // This example eliminates a separate class definition altogether with an anonymous class, so we
        // don't need--say--a PersonalDishAccumulator similar to PersonalIdentityAccumulator above
        Accumulator dishAcc = new Accumulator<Person,Dish,Map<String,String>,String>(
            (Map<String, String> map) -> map.get("dish"),          // rows should have a "dish" property
            Dish::getName,                                         // mapped to Dish's name attribute
            (Map<String, String> map) -> {                         // another liter closure example
                Dish dish = new Dish();
                dish.name = map.get("dish");
                return dish;
            }
        ) {
            @Override
            protected Optional<Dish> transition(Optional<Person> person, Map<String,String> row) {
                // My dilemna is whether to make this simply another closure to pass into the Accumulator. We already
                // pass 4. What's another one? Then we won't need to subclass Accumulator anymore.
                // However, maybe there will be cases when we do need to maintain some additional state in an
                // Accumulator (e.g. a summing/averaging Accumulator?). In which case, a subclass is required anyway.
                // Furthermore, the closures passed in currently deal mostly with transformation operations with
                // one consuming handler. The "chaining" aspect has mostly handled by the Accumulator class, so perhaps
                // the #transition() function should remain a method for that reason.
                Optional<Dish> dish = super.transition(person, row);
                if (person.isPresent() && dish.isPresent()) {
                    person.get().dishes.add(dish.get());
                }
                return dish;
            }
        };

        personAcc.withChained(identityAcc)
            .withChained(dishAcc);   // chain up the accumulators

        // The map rows model this data stream:

        // ID, firstName, lastName, dish, identity_type, identity_value
        // 100, Joe, Slim, Chicken, Driver's License, B1234567
        // 100, Joe, Slim, Chicken, SSN, 123-45-678
        // 101, Bob, Portly, Fish, Driver's License, C1234567
        // 101, Bob, Portly, Spinach, Driver's License, C1234567
        // 101, Bob, Portly, Steak, Driver's License, C1234567

        // A map is used for simplicity of the example, but the input data can be any streaming data source, such as
        // lines from a CSV file or rows of a ResultSet.

        Map<String,String> row = new HashMap<>();
        row.put("id", "100");
        row.put("firstName", "Joe");
        row.put("lastName", "Slim");
        row.put("dish", "Chicken");
        row.put("identity_type", "Driver's License");
        row.put("identity_value", "B1234567");
        personAcc.accumulate(row);          // 100, Joe, Slim, Chicken, Driver's License, B1234567

        row.put("identity_type", "SSN");
        row.put("identity_value", "123-45-678");
        personAcc.accumulate(row);          // 100, Joe, Slim, Chicken, SSN, 123-45-678

        row.clear();

        row.put("id", "101");
        row.put("firstName", "Bob");
        row.put("lastName", "Portly");
        row.put("dish", "Fish");
        row.put("identity_type", "Driver's License");
        row.put("identity_value", "C1234567");
        personAcc.accumulate(row);          // 101, Bob, Portly, Fish, Driver's License, C1234567

        row.put("dish", "Spinach");
        personAcc.accumulate(row);          // 101, Bob, Portly, Spinach, Driver's License, C1234567

        row.put("dish", "Steak");
        personAcc.accumulate(row);          // 101, Bob, Portly, Steak, Driver's License, C1234567

        // Signifies end of data (also necessary for the prev emitter to spit out the last value
        personAcc.accumulate(null);

        personAcc.accumulate(null);         // Subsequent calls should be no-ops

        System.out.println("people list: " + people.toString());
        System.out.println("peopleUsingPrevAcc list: " + peopleUsingPrevAcc.toString());
        // Output (formatted):

        // people list: [
        //   {100:Slim, Joe;
        //     Identities: [ {Driver's License:B1234567} {SSN:123-45-678} ];
        //     Dishes: [ {name:Chicken} ]},
        //   {101:Portly, Bob;
        //     Identities: [ {Driver's License:C1234567} ];
        //     Dishes: [ {name:Fish} {name:Spinach} {name:Steak} ]}
        // ]
        Assert.assertEquals("[{100:Slim, Joe; Identities: [ {Driver's License:B1234567} {SSN:123-45-678} ]; Dishes: [ {name:Chicken} ]}, {101:Portly, Bob; Identities: [ {Driver's License:C1234567} ]; Dishes: [ {name:Fish} {name:Spinach} {name:Steak} ]}]",
            people.toString());
        Assert.assertEquals(people, peopleUsingPrevAcc);

    }
}
