
export class User {
    name: string;
    childAccount: boolean;
    age: number;
    address: Address;
    addresses: Address[];
    taggedAddresses: { [index: string]: Address };
    groupedAddresses: { [index: string]: Address[] };
    listOfTaggedAddresses: { [index: string]: Address }[];
    orders: PagedList<Order>;
    allOrders: PagedList<Order>[];
    shape: ShapeUnion;
    shapes: ShapeUnion[];

    constructor(data?: User) {
        if (data) {
            this.name = data.name;
            this.childAccount = data.childAccount;
            this.age = data.age;
            this.address = data.address && new Address(data.address);
            this.addresses = __copyArray(data.addresses, data => new Address(data));
            this.taggedAddresses = __copyObject(data.taggedAddresses, data => new Address(data));
            this.groupedAddresses = __copyObject(data.groupedAddresses, data => __copyArray(data, data => new Address(data)));
            this.listOfTaggedAddresses = __copyArray(data.listOfTaggedAddresses, data => __copyObject(data, data => new Address(data)));
            this.orders = data.orders && new PagedList<Order>(data.orders);
            this.allOrders = __copyArray(data.allOrders, data => new PagedList<Order>(data));
            this.shape = Shape.__copyOf(data.shape);
            this.shapes = __copyArray(data.shapes, item => Shape.__copyOf(item));
        }
    }
}

export class Address {
    street: string;
    city: string;

    constructor(data?: Address) {
        if (data) {
            this.street = data.street;
            this.city = data.city;
        }
    }
}

export class PagedList<T> {
    page: number;
    items: T[];

    constructor(data?: PagedList<T>) {
        if (data) {
            this.page = data.page;
            this.items = data.items;
        }
    }
}

export class Order {
    id: string;

    constructor(data?: Order) {
        if (data) {
            this.id = data.id;
        }
    }
}

export class Shape {
    kind: "square" | "rectangle" | "circle";
    metadata: ShapeMetadata;

    constructor(data?: Shape) {
        if (data) {
            this.kind = data.kind;
            this.metadata = data.metadata;
        }
    }

    static __copyOf(data: ShapeUnion): ShapeUnion {
        if (!data) {
            return data;
        }
        switch (data.kind) {
            case "square":
                return new Square(data);
            case "rectangle":
                return new Rectangle(data);
            case "circle":
                return new Circle(data);
        }
    }
}

export class ShapeMetadata {
    group: string;

    constructor(data?: ShapeMetadata) {
        if (data) {
            this.group = data.group;
        }
    }
}

export class Square extends Shape {
    kind: "square";
    size: number;

    constructor(data?: Square) {
        super(data);
        if (data) {
            this.size = data.size;
        }
    }
}

export class Rectangle extends Shape {
    kind: "rectangle";
    width: number;
    height: number;

    constructor(data?: Rectangle) {
        super(data);
        if (data) {
            this.width = data.width;
            this.height = data.height;
        }
    }
}

export class Circle extends Shape {
    kind: "circle";
    radius: number;

    constructor(data?: Circle) {
        super(data);
        if (data) {
            this.radius = data.radius;
        }
    }
}

export type ShapeUnion = Square | Rectangle | Circle;

function __copyArray<T>(array: T[], copyFn: (item: T) => T): T[] {
    return array && array.map(item => item && copyFn(item));
}

function __copyObject<T>(object: { [index: string]: T }, copyFn: (item: T) => T) {
    if (!object) {
        return object;
    }
    const result: any = {};
    for (const key in object) {
        if (object.hasOwnProperty(key)) {
            const value = object[key];
            result[key] = value && copyFn(value);
        }
    }
    return result;
}
