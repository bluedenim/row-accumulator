# row-accumulate

Accumulator class to help partition and deserialize an ordered data stream (e.g. CSV lines, JDBC ResultSets) into
data beans, supporting chaining to build up collections of subpartitions.

Given a data stream of this format (example in CSV):
```
ID, firstName, lastName, dish, identity_type, identity_value
100, Joe, Slim, Chicken, Driver's License, B1234567
100, Joe, Slim, Chicken, SSN, 123-45-678
101, Bob, Portly, Fish, Driver's License, C1234567
101, Bob, Portly, Spinach, Driver's License, C1234567
101, Bob, Portly, Steak, Driver's License, C1234567
```

Three beans are created to model the top-level `Person` entity, then the children (siblings of one another) entities
for `Identity` and `Dish`. It is important that the data streams are ordered to group *key* properties in contiguous
*rows* of data. (This corresponds to the use of `ORDER BY` clauses in SQL.) For example the above may be achieved by
executing an SQL like:

```
select ID,firstName,lastName,dish,identity_type,identity_value
  from ...
  order by ID, dish, identity_type;
```

Correct use of the Accumulator class can produce a collection of `Person` instances those identities and dishes
collections contain the extracted entities. E.g.:

```
        people list: [
          {100:Slim, Joe;
            Identities: [ {Driver's License:B1234567} {SSN:123-45-678} ];
            Dishes: [ {name:Chicken} ]},
          {101:Portly, Bob;
            Identities: [ {Driver's License:C1234567} ];
            Dishes: [ {name:Fish} {name:Spinach} {name:Steak} ]}
        ]
```

The small class `Accumulator` makes heavy use of generics and closures in order to offer flexibility and reusability
to address different input sources, key calculation logic, and bean implementations. See the example domain classes
under the `org.van.rowaccumulate.example` package, starting with the unit test class
`org.van.rowaccumulate.AccumulatorTest`, for how to use it.

