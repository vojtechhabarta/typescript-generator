/* tslint:disable */

export class MultipleEnumContainerClass {
    readonly multiple: MultipleEntryEnum;

    constructor(multiple: MultipleEntryEnum) {
        this.multiple = multiple;
    }
}

export class SingleEnumContainerClass {
    readonly single: SingleEntryEnum;

    constructor() {
        this.single = "ENTRY_1";
    }
}

export type MultipleEntryEnum = "ENTRY_1" | "ENTRY_2" | "ENTRY_3";

export type SingleEntryEnum = "ENTRY_1";
