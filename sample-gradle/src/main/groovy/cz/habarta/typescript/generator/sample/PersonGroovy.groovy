
package cz.habarta.typescript.generator.sample

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("metaClass")
class PersonGroovy {
    public String name
    public int age
    public boolean hasChildren
    public List<String> tags
    public Map<String, String> emails
}
