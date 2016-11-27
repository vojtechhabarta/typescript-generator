package cz.habarta.typescript.generator;

public enum ClassType {

    asInterface("interface"), asClass("class");

    public final String keyword;

    ClassType(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
