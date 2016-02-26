
// declaration files

var a1: Test1Data;

var a2: NS2.Test2Data;

import * as mod3 from "mod3";
var a3: mod3.Test3Data;

import * as mod4 from "mod4";
var a4: mod4.NS4.Test4Data;


// implementation files

var a5: Test5Data;

var a6: NS6.Test6Data;
NS6.test();

import * as mod7 from "../../../target/test-ts-withmodule/test-mn7";
var a7: mod7.Test7Data;
mod7.test();

import * as mod8 from "../../../target/test-ts-withmodule/test-mn8";
var a8: mod8.NS8.Test8Data;
mod8.NS8.test();
