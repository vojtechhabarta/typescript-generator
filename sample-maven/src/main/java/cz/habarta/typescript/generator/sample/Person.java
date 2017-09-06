
package cz.habarta.typescript.generator.sample;

import java.util.*;
import javax.xml.bind.annotation.*;


public class Person {

    public String name;
    public int age;
    @XmlElement(name = "has-children", required = true)
    public boolean hasChildren;
    public List<String> tags;
    public Map<String, String> emails;

    @XmlTransient
    public String excluded;

}
