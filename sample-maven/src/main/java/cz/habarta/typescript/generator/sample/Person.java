
package cz.habarta.typescript.generator.sample;

import java.util.*;


public class Person {

    public String name;
    public Sex sex;
    public int age;
    public boolean hasChildren;
    public List<String> tags;
    public Map<String, String> emails;

}

enum Sex {
    MALE, FEMALE
}