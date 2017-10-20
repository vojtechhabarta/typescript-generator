
export class User {
    name: string;
    authentication: Authentication;
    childAccount: boolean;
    age: number;
    address: Address;
    addresses: Address[];
    taggedAddresses: { [index: string]: Address };
    groupedAddresses: { [index: string]: Address[] };
    listOfTaggedAddresses: { [index: string]: Address }[];
    orders: PagedList<Order, Authentication>;
    allOrders: PagedList<Order, Authentication>[];
    shape: ShapeUnion;
    shapes: ShapeUnion[];

    static fromData(data: User, target?: User): User {
        if (!data) {
            return data;
        }
        const instance = target || new User(); 
        instance.name = data.name;
        instance.authentication = data.authentication;
        instance.childAccount = data.childAccount;
        instance.age = data.age;
        instance.address = Address.fromData(data.address);
        instance.addresses = __copyArray(data.addresses, Address.fromData);
        instance.taggedAddresses = __copyObject(data.taggedAddresses, Address.fromData);
        instance.groupedAddresses = __copyObject(data.groupedAddresses, __copyArrayFn(Address.fromData));
        instance.listOfTaggedAddresses = __copyArray(data.listOfTaggedAddresses, __copyObjectFn(Address.fromData));
        instance.orders = PagedList.fromData(data.orders, Order.fromData, __identity);
        instance.allOrders = __copyArray(data.allOrders, PagedList.fromDataFn(Order.fromData, __identity));
        instance.shape = Shape.fromDataUnion(data.shape);
        instance.shapes = __copyArray(data.shapes, Shape.fromDataUnion);
        return instance;
    }
}

export class Address {
    street: string;
    city: string;

    static fromData(data: Address, target?: Address): Address {
        if (!data) {
            return data;
        }
        const instance = target || new Address();
        instance.street = data.street;
        instance.city = data.city;
        return instance;
    }
}

export class PagedList<T, A> {
    page: number;
    items: T[];
    additionalInfo: A;

    static fromDataFn<T, A>(constructorFnT: (data: T) => T, constructorFnA: (data: A) => A): (data: PagedList<T, A>) => PagedList<T, A> {
        return (data: PagedList<T, A>) => PagedList.fromData(data, constructorFnT, constructorFnA);
    }

    static fromData<T, A>(data: PagedList<T, A>, constructorFnT: (data: T) => T, constructorFnA: (data: A) => A, target?: PagedList<T, A>): PagedList<T, A> {
        if (!data) {
            return data;
        }
        const instance = target || new PagedList<T, A>();
        instance.page = data.page;
        instance.items = __copyArray(data.items, constructorFnT);
        instance.additionalInfo = constructorFnA(data.additionalInfo);
        return instance;
    }
}

export class Order {
    id: string;

    static fromData(data: Order, target?: Order): Order {
        if (!data) {
            return data;
        }
        const instance = target || new Order();
        instance.id = data.id;
        return instance;
    }
}

export class Shape {
    kind: "square" | "rectangle" | "circle";
    metadata: ShapeMetadata;

    static fromData(data: Shape, target?: Shape): Shape {
        if (!data) {
            return data;
        }
        const instance = target || new Shape();
        instance.kind = data.kind;
        instance.metadata = ShapeMetadata.fromData(data.metadata);
        return instance;
    }

    static fromDataUnion(data: ShapeUnion): ShapeUnion {
        if (!data) {
            return data;
        }
        switch (data.kind) {
            case "square":
                return Square.fromData(data);
            case "rectangle":
                return Rectangle.fromData(data);
            case "circle":
                return Circle.fromData(data);
        }
    }
}

export class ShapeMetadata {
    group: string;

    static fromData(data: ShapeMetadata, target?: ShapeMetadata): ShapeMetadata {
        if (!data) {
            return data;
        }
        const instance = target || new ShapeMetadata();
        instance.group = data.group;
        return instance;
    }
}

export class Square extends Shape {
    kind: "square";
    size: number;

    static fromData(data: Square, target?: Square): Square {
        if (!data) {
            return data;
        }
        const instance = target || new Square();
        super.fromData(data, instance);
        instance.size = data.size;
        return instance;
    }
}

export class Rectangle extends Shape {
    kind: "rectangle";
    width: number;
    height: number;

    static fromData(data: Rectangle, target?: Rectangle): Rectangle {
        if (!data) {
            return data;
        }
        const instance = target || new Rectangle();
        super.fromData(data, instance);
        instance.width = data.width;
        instance.height = data.height;
        return instance;
    }
}

export class Circle extends Shape {
    kind: "circle";
    radius: number;

    static fromData(data: Circle, target?: Circle): Circle {
        if (!data) {
            return data;
        }
        const instance = target || new Circle();
        super.fromData(data, instance);
        instance.radius = data.radius;
        return instance;
    }
}

export type Authentication = "Password" | "Token" | "Fingerprint" | "Voice";

export type ShapeUnion = Square | Rectangle | Circle;

function __copyArrayFn<T>(copyFn: (item: T) => T): (array: T[]) => T[] {
    return (array: T[]) => __copyArray(array, copyFn);
}

function __copyArray<T>(array: T[], copyFn: (item: T) => T): T[] {
    return array && array.map(item => item && copyFn(item));
}

function __copyObjectFn<T>(copyFn: (item: T) => T): (object: { [index: string]: T }) => { [index: string]: T } {
    return (object: { [index: string]: T }) => __copyObject(object, copyFn);
}

function __copyObject<T>(object: { [index: string]: T }, copyFn: (item: T) => T): { [index: string]: T } {
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

function __identity<T>(value: T): T {
    return value;
}
