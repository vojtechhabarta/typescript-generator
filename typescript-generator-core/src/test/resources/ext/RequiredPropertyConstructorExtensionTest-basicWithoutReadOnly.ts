/* tslint:disable */

export class PolymorphicClass implements SuperInterface {
    discriminator: "class-b";
    field1: number;
}

export class SimpleClass {
    field1: string;
    field2: PolymorphicClass;
}

export interface SuperInterface {
    discriminator: "class-b";
}
