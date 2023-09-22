# usaddress-java

usaddress-java is a Java port of [the usaddress Python library](https://github.com/datamade/usaddress) for parsing unstructured United States address strings into address components, using NLP methods.

**What this can do:** Using a probabilistic model, it makes educated guesses in identifying address components, even in tricky cases where rule-based parsers typically break down.

**What this cannot do:** It cannot identify address components with perfect accuracy, nor can it verify that a given address is correct/valid.

## How to use the usaddress-java library

1. Install the usaddress Maven dependency:

    ```xml
    <dependency>
      <groupId>io.github.dgileadi.usaddress</groupId>
      <artifactId>usaddress</artifactId>
      <version>1.0.0</version>
    </dependency>
    ```

2. Parse some addresses!

    Note that `parse` and `parseAndClean` are different methods:

    ```java
    import io.github.dgileadi.usaddress.Address;
    import io.github.dgileadi.usaddress.AddressParser;

    ..

    String address = "123 Main St. Suite 100 Chicago, IL";

    // The parse method will split your address string into components, and label each component.
    Address parsed = AddressParser.parse(address);

    // The parseAndClean method will try to be a little smarter.
    // It will merge consecutive components and strip commas.
    Address parsed = AddressParser.parseAndClean(address);
    ```

### Building & testing the code in this repo

To build a development version of usaddress on your machine, run the following code in your command line:

```sh
git clone https://github.com/dgileadi/usaddress-java.git
cd usaddress-java
mvn clean install
```

## Copyright

Copyright (c) 2023 David Gileadi.

Original code copyright (c) 2014 Atlanta Journal Constitution.

Released under the [MIT License](https://github.com/dgileadi/usaddress-java/blob/master/LICENSE).
