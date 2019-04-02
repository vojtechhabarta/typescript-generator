/* tslint:disable */

export class PolymorphicClass implements SuperInterface {
    class: "class-b";
    field1: number;
}

export class SimpleClass {
    field1: string;
    field2: PolymorphicClass;
}

export interface SuperInterface {
    class: "class-b";
}
