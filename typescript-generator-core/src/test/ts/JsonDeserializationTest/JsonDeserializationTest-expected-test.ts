import { Address, Order, PagedList, Rectangle, ShapeMetadata, Square, User } from "../../resources/cz/habarta/typescript/generator/JsonDeserializationTest-expected";

// load JSON data
const data = require("./JsonDeserializationTest-expected-test-data");
assertEquals(typeof data, "object");
assertEquals(data instanceof User, false);

// deserialize to classes
const user = User.fromData(data);

assertType(user, "object");
assertType(user, User);

// .name
assertType(user.name, "string");

// .authentication
assertType(user.authentication, "string");

// .childAccount
assertType(user.childAccount, "boolean");

// .age
assertType(user.age, "number");

// .address
assertType(user.address, Address);

// .addresses
assertType(user.addresses, "object"/*array*/);
assertType(user.addresses, Array);

// .taggedAddresses
assertType(user.taggedAddresses, "object");
assertType(user.taggedAddresses["address3"], Address);

// .groupedAddresses
assertType(user.groupedAddresses, "object");
assertType(user.groupedAddresses["addresses4"], Array);
assertType(user.groupedAddresses["addresses4"][0], Address);

// .listOfTaggedAddresses
assertType(user.listOfTaggedAddresses, Array);
assertType(user.listOfTaggedAddresses[0], "object");
assertType(user.listOfTaggedAddresses[0]["address6"], Address);

// .tags
assertType(user.tags, Array);
assertType(user.tags[0], "string");

// .mapping
assertType(user.mapping, "object");
assertType(user.mapping["key"], "string");

// .listOfListOfString
assertType(user.listOfListOfString, Array);
assertType(user.listOfListOfString[0], Array);
assertType(user.listOfListOfString[0][0], "string");

// .orders
assertType(user.orders, PagedList);
assertType(user.orders.page, "number");
assertType(user.orders.items, Array);
assertType(user.orders.items[0], Order);
assertType(user.orders.items[0].id, "string");
assertType(user.orders.additionalInfo, "string");

// .allOrders
assertType(user.allOrders, Array);
assertType(user.allOrders[0], PagedList);
assertType(user.allOrders[0].page, "number");
assertType(user.allOrders[0].items, Array);
assertType(user.allOrders[0].items[0], Order);
assertType(user.allOrders[0].items[0].id, "string");
assertType(user.allOrders[0].additionalInfo, "string");

// .shape
assertType(user.shape, "object");
assertType(user.shape, Square);
assertType((user.shape as Square).kind, "string");
assertType((user.shape as Square).metadata, ShapeMetadata);
assertType((user.shape as Square).metadata.group, "string");
assertType((user.shape as Square).size, "number");

// .shapes
assertType(user.shapes, Array);
assertType(user.shapes[0], "object");
assertType(user.shapes[0], Rectangle);
assertType((user.shapes[0] as Rectangle).kind, "string");
assertType((user.shapes[0] as Rectangle).metadata, ShapeMetadata);
assertType((user.shapes[0] as Rectangle).metadata.group, "string");
assertType((user.shapes[0] as Rectangle).width, "number");
assertType((user.shapes[0] as Rectangle).height, "number");


// augment User
declare module "../../resources/cz/habarta/typescript/generator/JsonDeserializationTest-expected" {
    interface User {
        equals(other: User): boolean;
    }
}
User.prototype.equals = function (this: User, other) {
    return this.name === other.name;
};

// test augmentation
const user1 = User.fromData(data);
const user2 = User.fromData(data);
assertEquals(user1.equals(user2), true);
user2.name = "name2";
assertEquals(user1.equals(user2), false);


console.log("Test finished.");


function assertType(value: any, type: string | Function, message?: string) {
    if (typeof type === "string") {
        assertEquals(typeof value, type, message);
    } else {
        assertEquals(value instanceof type, true, message);
    }
}

function assertEquals(actual: any, expected: any, message?: string) {
    if (actual !== expected) {
        fail(
            (message ? message + " " : "") +
            `"expected [${expected}] but found [${actual}]`
        );
    }
}

function fail(message: string) {
    throw new Error(message);
}
