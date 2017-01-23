
/// <reference path="../../../target/test-ts-modules/test-mn1.d.ts" />
/// <reference path="../../../target/test-ts-modules/test-mn2.d.ts" />
/// <reference path="../../../target/test-ts-modules/test-mn3a.d.ts" />
/// <reference path="../../../target/test-ts-modules/test-mn4a.d.ts" />
/// <reference path="../../../target/test-ts-modules/test-mn5.ts" />
/// <reference path="../../../target/test-ts-modules/test-mn6.ts" />

// declaration files

var a1: Test1Data;
var e1: Test1Direction;
var ne1: Test1Align;

var a2: NS2.Test2Data;
var e2: NS2.Test2Direction;
var ne2: NS2.Test2Align;

import * as mod3a from "mod3a";
var a3a: mod3a.Test3aData;
var e3a: mod3a.Test3aDirection;
var ne3a: mod3a.Test3aAlign;

import * as mod3b from "../../../target/test-ts-modules/test-mn3b";
var a3b: mod3b.Test3bData;
var e3b: mod3b.Test3bDirection;
var ne3b: mod3b.Test3bAlign;

import * as mod4a from "mod4a";
var a4a: mod4a.NS4a.Test4aData;
var e4a: mod4a.NS4a.Test4aDirection;
var ne4a: mod4a.NS4a.Test4aAlign;

import * as mod4b from "../../../target/test-ts-modules/test-mn4b";
var a4b: mod4b.NS4b.Test4bData;
var e4b: mod4b.NS4b.Test4bDirection;
var ne4b: mod4b.NS4b.Test4bAlign;


// implementation files

var a5: Test5Data;
var e5: Test5Direction;
var ne5: Test5Align;
test();

var a6: NS6.Test6Data;
var e6: NS6.Test6Direction;
var ne6: NS6.Test6Align;
NS6.test();

import * as mod7 from "../../../target/test-ts-modules/test-mn7";
var a7: mod7.Test7Data;
var e7: mod7.Test7Direction;
var ne7: mod7.Test7Align;
mod7.test();

import * as mod8 from "../../../target/test-ts-modules/test-mn8";
var a8: mod8.NS8.Test8Data;
var e8: mod8.NS8.Test8Direction;
var ne8: mod8.NS8.Test8Align;
mod8.NS8.test();
