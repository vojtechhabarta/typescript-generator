/* tslint:disable */

export class SimpleOptionalClass {
    readonly field1: string;
    readonly field2?: number;

    constructor(field1: string, field2?: number) {
        this.field1 = field1;
        this.field2 = field2;
    }
}

export class SecondOptionalClass extends SimpleOptionalClass {
    readonly field3: string;

    constructor(field1: string, field3: string, field2?: number) {
        super(field1, field2);
        this.field3 = field3;
    }
}
