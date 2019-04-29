/* tslint:disable */

export class OtherClass {
    readonly field2: string;

    constructor(field2: string) {
        this.field2 = field2;
    }
}

export class PolymorphicClass implements SuperInterface {
    readonly discriminator: "class-b";
    readonly field1: number;
}

export class SimpleClass {
    readonly field1: string;
    readonly field2: PolymorphicClass;

    constructor(field1: string, field2: PolymorphicClass) {
        this.field1 = field1;
        this.field2 = field2;
    }
}

export interface SuperInterface {
    readonly discriminator: "class-b";
}
