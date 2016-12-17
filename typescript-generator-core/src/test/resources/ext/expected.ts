
export class Fields {
    protected $$parent: Fields | undefined;
    protected $$name: string;
    constructor(parent?: Fields, name?: string) {
        this.$$parent = parent;
        this.$$name = name || '';
    };
    get(): string {
        if (this.$$parent && this.$$parent.get().length > 0) {
            return this.$$parent.get() + "." + this.$$name;
        } else {
            return this.$$name;
        }
    }
}

class ClassAFields extends Fields {
    constructor(parent?: Fields, name?: string) { super(parent, name); }
    field1 = new Fields(this, "field1");
    field2 = new ClassBFields(this, "field2");
    field3 = new ClassCFields(this, "field3");
}

class ClassBFields extends Fields {
    constructor(parent?: Fields, name?: string) { super(parent, name); }
    field1 = new Fields(this, "field1");
}

class ClassCFields extends ClassBFields {
    constructor(parent?: Fields, name?: string) { super(parent, name); }
    field4 = new Fields(this, "field4");
}
const ClassA = new ClassAFields();
const ClassB = new ClassBFields();
const ClassC = new ClassCFields();
