
// declaration files

var a1: Test1Data;
var e1: Test1Direction;

var a2: NS2.Test2Data;
var e2: NS2.Test2Direction;

import * as mod3a from "mod3a";
var a3a: mod3a.Test3aData;
var e3a: mod3a.Test3aDirection;

import * as mod3b from "../../../target/test-ts-modules/test-mn3b";
var a3b: mod3b.Test3bData;
var e3b: mod3b.Test3bDirection;

import * as mod4a from "mod4a";
var a4a: mod4a.NS4a.Test4aData;
var e4a: mod4a.NS4a.Test4aDirection;

import * as mod4b from "../../../target/test-ts-modules/test-mn4b";
var a4b: mod4b.NS4b.Test4bData;
var e4b: mod4b.NS4b.Test4bDirection;


// implementation files

var a5: Test5Data;
var e5: Test5Direction;
test();

var a6: NS6.Test6Data;
var e6: NS6.Test6Direction;
NS6.test();

import * as mod7 from "../../../target/test-ts-modules/test-mn7";
var a7: mod7.Test7Data;
var e7: mod7.Test7Direction;
mod7.test();

import * as mod8 from "../../../target/test-ts-modules/test-mn8";
var a8: mod8.NS8.Test8Data;
var e8: mod8.NS8.Test8Direction;
mod8.NS8.test();
