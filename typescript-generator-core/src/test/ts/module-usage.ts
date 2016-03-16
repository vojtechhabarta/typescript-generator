
// declaration files

var a1: Test1Data;

var a2: NS2.Test2Data;

import * as mod3a from "mod3a";
var a3a: mod3a.Test3aData;

import * as mod3b from "../../../target/test-ts-modules/test-mn3b";
var a3b: mod3b.Test3bData;

import * as mod4a from "mod4a";
var a4a: mod4a.NS4a.Test4aData;

import * as mod4b from "../../../target/test-ts-modules/test-mn4b";
var a4b: mod4b.NS4b.Test4bData;


// implementation files

var a5: Test5Data;
test();

var a6: NS6.Test6Data;
NS6.test();

import * as mod7 from "../../../target/test-ts-modules/test-mn7";
var a7: mod7.Test7Data;
mod7.test();

import * as mod8 from "../../../target/test-ts-modules/test-mn8";
var a8: mod8.NS8.Test8Data;
mod8.NS8.test();
