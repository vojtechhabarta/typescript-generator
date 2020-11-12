
export class BaseClass {
    discriminator: "OnePossiblePropertyValueAssigningExtensionTest$SubClass" | "OnePossiblePropertyValueAssigningExtensionTest$OtherSubClass";
    field1: number;
    readonly field2: OneValueEnum;

    constructor() {
        this.field2 = "MY_VALUE";
    }
}

export class OtherSubClass extends BaseClass {
    readonly discriminator: "OnePossiblePropertyValueAssigningExtensionTest$OtherSubClass";
    readonly enumField1: OneValueEnum;
    enumField2: TwoValueEnum;
    testField2: string;

    constructor() {
        super();
        this.discriminator = "OnePossiblePropertyValueAssigningExtensionTest$OtherSubClass";
        this.enumField1 = "MY_VALUE";
    }
}

export class SubClass extends BaseClass {
    readonly discriminator: "OnePossiblePropertyValueAssigningExtensionTest$SubClass";
    testField1: string;

    constructor() {
        super();
        this.discriminator = "OnePossiblePropertyValueAssigningExtensionTest$SubClass";
    }
}

export type OneValueEnum = "MY_VALUE";

export type TwoValueEnum = "ONE" | "TWO";
