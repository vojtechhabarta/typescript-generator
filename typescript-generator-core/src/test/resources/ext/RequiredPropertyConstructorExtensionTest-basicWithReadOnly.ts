/* tslint:disable */

export class PolymorphicClass implements SuperInterface {
    readonly discriminator: "class-b";
    readonly field1: number;

    constructor(field1: number) {
        this.discriminator = "class-b";
        this.field1 = field1;
    }
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
